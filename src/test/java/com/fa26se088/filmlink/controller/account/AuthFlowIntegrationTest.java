package com.fa26se088.filmlink.controller.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.client.notification.EmailClient;
import com.fa26se088.filmlink.entity.account.OtpCode;
import com.fa26se088.filmlink.repository.account.OtpCodeRepository;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Luồng UC06 → UC01 chạy trên DB thật (schema test): đăng ký → OTP → xác thực email → /me →
 * refresh → đăng xuất. {@link EmailClient} được thay bằng mock để đọc mã OTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

  private static final String PASSWORD = "secret123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private OtpCodeRepository otpCodeRepository;

  @MockitoBean private EmailClient emailClient;

  private final String email = "it-" + UUID.randomUUID() + "@example.com";

  /** Xoá tài khoản thử; refresh_tokens và otp_codes bị xoá theo (ON DELETE CASCADE). */
  @AfterEach
  void cleanUp() {
    userAccountRepository.findByEmail(email).ifPresent(userAccountRepository::delete);
  }

  @Test
  void registerVerifyRefreshLogoutFlow() throws Exception {
    // UC06 bước 1–2: đăng ký, tài khoản chưa xác thực, không có mật khẩu trong response.
    postJson("/api/v1/auth/register", registerBody(email.toUpperCase(Locale.ROOT)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("PHOTOGRAPHER"))
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    String otp = capturedOtp();

    // UC01: chưa xác thực email thì không đăng nhập được.
    postJson("/api/v1/auth/login", loginBody(PASSWORD))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_VERIFIED"));

    // OTP sai: 422 MSG11, số lần sai được lưu dù request lỗi.
    postJson("/api/v1/auth/verify-email", verifyBody(otp.equals("000000") ? "111111" : "000000"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_OTP_INVALID"))
        .andExpect(jsonPath("$.messageCode").value("MSG11"));
    assertThat(otpCodeRepository.findAll())
        .filteredOn(code -> code.getUserAccount().getId().equals(accountId()))
        .extracting(OtpCode::getFailedAttempts)
        .containsExactly((short) 1);

    // UC06 bước 3: OTP đúng → kích hoạt và tự đăng nhập.
    String tokens =
        postJson("/api/v1/auth/verify-email", verifyBody(otp))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonPath.read(tokens, "$.accessToken");
    String firstRefreshToken = JsonPath.read(tokens, "$.refreshToken");

    mockMvc
        .perform(get("/api/v1/accounts/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.emailVerifiedAt").isNotEmpty());

    // NFR-SEC-01: refresh xoay vòng, token cũ không dùng lại được.
    String rotated =
        postJson("/api/v1/auth/refresh", refreshBody(firstRefreshToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secondRefreshToken = JsonPath.read(rotated, "$.refreshToken");
    String secondAccessToken = JsonPath.read(rotated, "$.accessToken");
    assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

    postJson("/api/v1/auth/refresh", refreshBody(firstRefreshToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_TOKEN"));

    // UC01 bước 3: đăng xuất thu hồi refresh token.
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, bearer(secondAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(secondRefreshToken)))
        .andExpect(status().isNoContent());

    postJson("/api/v1/auth/refresh", refreshBody(secondRefreshToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_TOKEN"));
  }

  @Test
  void loginAfterVerification() throws Exception {
    postJson("/api/v1/auth/register", registerBody(email)).andExpect(status().isCreated());
    postJson("/api/v1/auth/verify-email", verifyBody(capturedOtp())).andExpect(status().isOk());

    postJson("/api/v1/auth/login", loginBody(PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());

    // MSG09: một lỗi chung cho sai mật khẩu.
    postJson("/api/v1/auth/login", loginBody("wrong-pass1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.messageCode").value("MSG09"));

    // UC06: email trùng trả lỗi chung chung.
    postJson("/api/v1/auth/register", registerBody(email))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_REGISTRATION_REJECTED"));
  }

  @Test
  void invalidRegisterBodyReturnsFieldErrors() throws Exception {
    String body =
        """
        {"email":"not-an-email","password":"onlyletters","fullName":"A","termsAccepted":false}
        """;
    postJson("/api/v1/auth/register", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'termsAccepted')]").exists());
  }

  private ResultActions postJson(String path, String body) throws Exception {
    return mockMvc.perform(
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String capturedOtp() {
    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailClient).send(eq(email), anyString(), bodyCaptor.capture());
    Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(bodyCaptor.getValue());
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }

  private Long accountId() {
    return userAccountRepository.findByEmail(email).orElseThrow().getId();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String registerBody(String email) {
    return """
        {"email":"%s","password":"%s","fullName":"Nguyen Van An","termsAccepted":true}
        """
        .formatted(email, PASSWORD);
  }

  private String loginBody(String password) {
    return """
        {"email":"%s","password":"%s"}
        """.formatted(email, password);
  }

  private String verifyBody(String otp) {
    return """
        {"email":"%s","otp":"%s"}
        """.formatted(email, otp);
  }

  private static String refreshBody(String refreshToken) {
    return """
        {"refreshToken":"%s"}
        """.formatted(refreshToken);
  }
}
