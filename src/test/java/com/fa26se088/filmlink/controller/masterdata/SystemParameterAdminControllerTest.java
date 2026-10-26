package com.fa26se088.filmlink.controller.masterdata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.dto.masterdata.request.UpdateSystemParameterRequest;
import com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.entity.masterdata.enums.ParameterValueType;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import com.fa26se088.filmlink.service.masterdata.SystemParameterAdminService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemParameterAdminControllerTest {

  private static final String BASE_PATH = "/api/v1/admin/system-parameters";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private UserAccountRepository userAccountRepository;

  @MockitoBean private SystemParameterAdminService systemParameterAdminService;

  private final List<UserAccount> createdAccounts = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    userAccountRepository.deleteAll(createdAccounts);
  }

  @Test
  @DisplayName("GET /api/v1/admin/system-parameters không có token trả 401")
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/v1/admin/system-parameters với role PHOTOGRAPHER trả 403 Forbidden")
  void getAll_forbiddenRole_returns403() throws Exception {
    mockMvc
        .perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.PHOTOGRAPHER)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/v1/admin/system-parameters với ADMIN trả 200 OK")
  void getAll_admin_returns200() throws Exception {
    when(systemParameterAdminService.getAllParameters())
        .thenReturn(
            List.of(
                new SystemParameterResponse(
                    1L,
                    SystemParameterKey.DEFAULT_COMMISSION_RATE,
                    "10",
                    ParameterValueType.PERCENT,
                    "Commission",
                    0,
                    null,
                    Instant.now())));

    mockMvc
        .perform(get(BASE_PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].key").value("DEFAULT_COMMISSION_RATE"))
        .andExpect(jsonPath("$[0].value").value("10"));
  }

  @Test
  @DisplayName("GET /api/v1/admin/system-parameters/{key} với ADMIN trả 200 OK")
  void getByKey_admin_returns200() throws Exception {
    when(systemParameterAdminService.getParameter(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(
            new SystemParameterResponse(
                1L,
                SystemParameterKey.DEFAULT_COMMISSION_RATE,
                "10",
                ParameterValueType.PERCENT,
                "Commission",
                0,
                null,
                Instant.now()));

    mockMvc
        .perform(
            get(BASE_PATH + "/DEFAULT_COMMISSION_RATE")
                .header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("DEFAULT_COMMISSION_RATE"))
        .andExpect(jsonPath("$.value").value("10"));
  }

  @Test
  @DisplayName("PUT /api/v1/admin/system-parameters/{key} cập nhật thành công trả 200 OK")
  void updateParameter_admin_returns200() throws Exception {
    when(systemParameterAdminService.updateParameter(
            eq(SystemParameterKey.DEFAULT_COMMISSION_RATE),
            any(UpdateSystemParameterRequest.class),
            any()))
        .thenReturn(
            new SystemParameterResponse(
                1L,
                SystemParameterKey.DEFAULT_COMMISSION_RATE,
                "15",
                ParameterValueType.PERCENT,
                "Commission",
                1,
                99L,
                Instant.now()));

    String body = """
        {
          "value": "15",
          "version": 0
        }
        """;

    mockMvc
        .perform(
            put(BASE_PATH + "/DEFAULT_COMMISSION_RATE")
                .header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("15"))
        .andExpect(jsonPath("$.version").value(1));

    verify(systemParameterAdminService)
        .updateParameter(
            eq(SystemParameterKey.DEFAULT_COMMISSION_RATE),
            any(UpdateSystemParameterRequest.class),
            any());
  }

  @Test
  @DisplayName("PUT /api/v1/admin/system-parameters/{key} thiếu version trả 400 VALIDATION_ERROR")
  void updateParameter_missingVersion_returns400() throws Exception {
    String body = """
        {
          "value": "15"
        }
        """;

    mockMvc
        .perform(
            put(BASE_PATH + "/DEFAULT_COMMISSION_RATE")
                .header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  private String bearer(UserAccountRole role) {
    UserAccount account = new UserAccount();
    account.setEmail("admin-sysparam-" + UUID.randomUUID() + "@example.com");
    account.setPasswordHash("hash");
    account.setFullName("SysParam Admin");
    account.setRole(role);
    account.setStatus(UserAccountStatus.ACTIVE);
    account.setTermsAcceptedAt(Instant.now());
    createdAccounts.add(userAccountRepository.save(account));
    return "Bearer "
        + jwtTokenProvider.generateAccessToken(account.getId(), account.getEmail(), role);
  }
}
