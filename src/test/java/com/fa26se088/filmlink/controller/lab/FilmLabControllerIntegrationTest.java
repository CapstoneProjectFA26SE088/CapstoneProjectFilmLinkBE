package com.fa26se088.filmlink.controller.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.client.storage.StorageClient;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import com.fa26se088.filmlink.service.lab.impl.FilmLabFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code /api/v1/film-labs/me} trên DB thật (schema test): Film Lab Owner xem/sửa hồ sơ, giờ mở
 * cửa, ảnh mẫu, bật/tắt nhận đơn (UC12, BR-57). Storage test ghi vào thư mục tạm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilmLabControllerIntegrationTest {

  private static final String PATH = "/api/v1/film-labs/me";
  private static final String DESCRIPTION =
      "Lab tráng phim màu C-41, đen trắng và E-6; scan bằng Noritsu HS-1800 tại quận 1.";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private StorageClient storageClient;

  private FilmLabFixture fixture;
  private UserAccount owner;
  private long labId;

  @BeforeEach
  void setUp() {
    fixture =
        new FilmLabFixture(
            jdbcTemplate, userAccountRepository, eventPublisher, transactionTemplate, storageClient);
    owner = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);
    labId = fixture.createLab(owner, "Lab Chủ A");
  }

  @AfterEach
  void cleanUp() {
    fixture.cleanUp();
  }

  // --- Quyền ---

  @Test
  void anonymousGets401() throws Exception {
    mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  void photographerGets403() throws Exception {
    UserAccount photographer = fixture.createAccount(UserAccountRole.PHOTOGRAPHER);

    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(photographer)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
  }

  @Test
  void ownerWithoutLabGets404() throws Exception {
    UserAccount ownerWithoutLab = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);

    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(ownerWithoutLab)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("LAB_NOT_FOUND"));
  }

  // --- Hồ sơ ---

  @Test
  void getProfile_newLab() throws Exception {
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(labId))
        .andExpect(jsonPath("$.labName").value("Lab Chủ A"))
        .andExpect(jsonPath("$.acceptingOrders").value(false))
        .andExpect(jsonPath("$.qcBufferHours").value(4))
        .andExpect(jsonPath("$.openingHours", hasSize(0)))
        .andExpect(jsonPath("$.images", hasSize(0)));
  }

  @Test
  void putProfile_valid_updatesProfile() throws Exception {
    mockMvc
        .perform(
            put(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileJson("10.776889", "106.700806")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.labName").value("Lab Chủ A Mới"))
        .andExpect(jsonPath("$.description").value(DESCRIPTION))
        .andExpect(jsonPath("$.latitude").value(10.776889))
        .andExpect(jsonPath("$.longitude").value(106.700806))
        .andExpect(jsonPath("$.qcBufferHours").value(6))
        .andExpect(jsonPath("$.expressDailyCapPercent").value(30))
        .andExpect(jsonPath("$.contactEmail").value("lab@example.com"));
  }

  @Test
  void putProfile_onlyLatitude_returns400() throws Exception {
    mockMvc
        .perform(
            put(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileJson("10.776889", null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void putProfile_sevenDecimalPlaces_returns400() throws Exception {
    mockMvc
        .perform(
            put(PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileJson("10.7768891", "106.700806")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("latitude"));
  }

  // --- Giờ mở cửa ---

  @Test
  void putOpeningHours_valid_thenReplace() throws Exception {
    mockMvc
        .perform(openingHours("08:00", "18:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openingHours", hasSize(7)))
        .andExpect(jsonPath("$.openingHours[0].dayOfWeek").value(1))
        .andExpect(jsonPath("$.openingHours[0].opensAt").value("08:00"))
        .andExpect(jsonPath("$.openingHours[0].closesAt").value("18:00"))
        .andExpect(jsonPath("$.openingHours[6].closed").value(true))
        .andExpect(jsonPath("$.openingHours[6].opensAt").doesNotExist());

    // Gửi lại: sửa dòng cũ theo ngày, không tạo thêm dòng.
    mockMvc
        .perform(openingHours("09:30", "17:00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openingHours", hasSize(7)))
        .andExpect(jsonPath("$.openingHours[0].opensAt").value("09:30"));
  }

  @Test
  void putOpeningHours_opensAfterCloses_returns400() throws Exception {
    mockMvc
        .perform(openingHours("18:00", "08:00"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  void putOpeningHours_sixDays_returns400() throws Exception {
    StringBuilder sixDays = new StringBuilder("{\"openingHours\":[");
    for (int day = 1; day <= 6; day++) {
      sixDays.append(day == 1 ? "" : ",").append(day(day, "08:00", "18:00"));
    }
    sixDays.append("]}");

    mockMvc
        .perform(
            put(PATH + "/opening-hours")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sixDays.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("openingHours"));
  }

  // --- Ảnh mẫu ---

  @Test
  void uploadAndDeleteImages_reusesSmallestFreeDisplayOrder() throws Exception {
    long first = uploadImage(owner, 1);
    uploadImage(owner, 2);

    mockMvc
        .perform(
            delete(PATH + "/images/" + first).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(status().isNoContent());

    uploadImage(owner, 1);
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.images", hasSize(2)))
        .andExpect(jsonPath("$.images[0].displayOrder").value(1))
        .andExpect(jsonPath("$.images[1].displayOrder").value(2))
        .andExpect(jsonPath("$.images[0].url").value(startsWith("http")));
  }

  @Test
  void uploadImage_notAnImage_returns400() throws Exception {
    MockMultipartFile text =
        new MockMultipartFile(
            "file", "note.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart(PATH + "/images").file(text).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_FILE_TYPE_NOT_ALLOWED"));
  }

  /** CR-14: owner A không xoá được ảnh của lab B, và chỉ thấy lab của mình. */
  @Test
  void ownerCannotTouchAnotherLab() throws Exception {
    UserAccount ownerB = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);
    fixture.createLab(ownerB, "Lab Chủ B");
    long imageOfB = uploadImage(ownerB, 1);

    mockMvc
        .perform(
            delete(PATH + "/images/" + imageOfB).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("LAB_IMAGE_NOT_FOUND"));

    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
        .andExpect(jsonPath("$.labName").value("Lab Chủ A"))
        .andExpect(jsonPath("$.images", hasSize(0)));
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(ownerB)))
        .andExpect(jsonPath("$.labName").value("Lab Chủ B"))
        .andExpect(jsonPath("$.images", hasSize(1)));
  }

  // --- Nhận đơn (BR-57) ---

  @Test
  void acceptingOrders_requiresActiveProcessorAndScanner() throws Exception {
    mockMvc
        .perform(acceptingOrders(true))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.errorCode").value("LAB_MACHINES_NOT_READY"))
        .andExpect(jsonPath("$.messageCode").value("MSG32"));

    // Máy scan đang bảo trì không được tính.
    fixture.insertMachine(labId, "PROCESSOR", "ACTIVE");
    fixture.insertMachine(labId, "SCANNER", "MAINTENANCE");
    mockMvc
        .perform(acceptingOrders(true))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.errorCode").value("LAB_MACHINES_NOT_READY"));

    fixture.insertMachine(labId, "SCANNER", "ACTIVE");
    mockMvc
        .perform(acceptingOrders(true))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptingOrders").value(true));

    mockMvc
        .perform(acceptingOrders(false))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptingOrders").value(false));

    Integer auditRows =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM audit_logs WHERE action = 'LAB_ACCEPTING_ORDERS_CHANGED'"
                + " AND target_type = 'FILM_LAB' AND target_id = ? AND actor_account_id = ?",
            Integer.class,
            labId,
            owner.getId());
    assertThat(auditRows).isEqualTo(2);
  }

  // --- helpers ---

  private long uploadImage(UserAccount account, int expectedDisplayOrder) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", png());
    String body =
        mockMvc
            .perform(
                multipart(PATH + "/images")
                    .file(file)
                    .header(HttpHeaders.AUTHORIZATION, bearer(account)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayOrder").value(expectedDisplayOrder))
            .andExpect(jsonPath("$.url").value(startsWith("http")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private RequestBuilder openingHours(
      String opensAt, String closesAt) {
    StringBuilder json = new StringBuilder("{\"openingHours\":[");
    for (int day = 1; day <= 6; day++) {
      json.append(day(day, opensAt, closesAt)).append(',');
    }
    json.append("{\"dayOfWeek\":7,\"closed\":true}]}");
    return put(PATH + "/opening-hours")
        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.toString());
  }

  private static String day(int day, String opensAt, String closesAt) {
    return "{\"dayOfWeek\":%d,\"closed\":false,\"opensAt\":\"%s\",\"closesAt\":\"%s\"}"
        .formatted(day, opensAt, closesAt);
  }

  private RequestBuilder acceptingOrders(boolean value) {
    return patch(PATH + "/accepting-orders")
        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"acceptingOrders\":" + value + "}");
  }

  private static String profileJson(String latitude, String longitude) {
    return """
        {"labName":"Lab Chủ A Mới","description":"%s","addressLine":"12 Nguyễn Huệ",
         "district":"Quận 1","province":"TP. Hồ Chí Minh","latitude":%s,"longitude":%s,
         "qcBufferHours":6,"expressDailyCapPercent":30}
        """
        .formatted(DESCRIPTION, latitude, longitude);
  }

  /** PNG tối thiểu: chỉ cần đúng magic bytes để storage nhận. */
  private static byte[] png() {
    byte[] content = new byte[256];
    byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(header, 0, content, 0, header.length);
    return content;
  }

  private String bearer(UserAccount account) {
    return "Bearer "
        + jwtTokenProvider.generateAccessToken(
            account.getId(), account.getEmail(), account.getRole());
  }
}
