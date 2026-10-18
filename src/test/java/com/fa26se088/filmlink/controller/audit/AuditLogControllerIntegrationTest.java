package com.fa26se088.filmlink.controller.audit;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.security.JwtTokenProvider;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /api/v1/admin/audit-logs} trên DB thật (schema test): chỉ ADMIN được xem (BR-32,
 * CR-16), lọc theo targetType + targetId, mới nhất trước.
 *
 * <p>Tài khoản thử được xoá sau mỗi test (không dòng audit nào tham chiếu tới chúng). Dòng audit
 * dùng {@code targetId} ngẫu nhiên vì không xoá được.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerIntegrationTest {

  private static final String PATH = "/api/v1/admin/audit-logs";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private AuditLogService auditLogService;

  private final List<UserAccount> createdAccounts = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    userAccountRepository.deleteAll(createdAccounts);
  }

  @Test
  void adminCanListAuditLogs() throws Exception {
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void photographerGets403() throws Exception {
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.PHOTOGRAPHER)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
  }

  /** BR-05 chỉ cho ADMIN ⊃ MODERATOR, không ngược lại: Moderator không xem được audit log. */
  @Test
  void moderatorGets403() throws Exception {
    mockMvc
        .perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.MODERATOR)))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousGets401() throws Exception {
    mockMvc
        .perform(get(PATH))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  void filterByTargetTypeAndTargetIdNewestFirst() throws Exception {
    long targetId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE - 1);
    auditLogService.recordSuccess(entry(targetId, "lần 1"));
    auditLogService.recordFailure(entry(targetId, "lần 2"));
    auditLogService.recordSuccess(entry(targetId + 1, "đối tượng khác"));

    mockMvc
        .perform(
            get(PATH)
                .param("targetType", "USER_ACCOUNT")
                .param("targetId", String.valueOf(targetId))
                .param("sort", "occurredAt,asc")
                .header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[*].targetId").value(everyItem(is(targetId))))
        .andExpect(jsonPath("$.content[*].targetType").value(everyItem(is("USER_ACCOUNT"))))
        // Sort cố định occurredAt DESC, bỏ qua sort client gửi lên.
        .andExpect(jsonPath("$.content[0].reason").value("lần 2"))
        .andExpect(jsonPath("$.content[0].outcome").value("FAILURE"))
        .andExpect(jsonPath("$.content[1].reason").value("lần 1"))
        .andExpect(jsonPath("$.content[1].outcome").value("SUCCESS"));
  }

  @Test
  void unknownTargetTypeGets400() throws Exception {
    mockMvc
        .perform(
            get(PATH)
                .param("targetType", "NOT_A_TYPE")
                .header(HttpHeaders.AUTHORIZATION, bearer(UserAccountRole.ADMIN)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  private static AuditEntry entry(long targetId, String reason) {
    return new AuditEntry(
        AuditAction.ACCOUNT_PASSWORD_CHANGED,
        AuditTargetType.USER_ACCOUNT,
        targetId,
        reason,
        null,
        null);
  }

  private String bearer(UserAccountRole role) {
    UserAccount account = new UserAccount();
    account.setEmail("audit-it-" + UUID.randomUUID() + "@example.com");
    account.setPasswordHash("not-a-real-hash");
    account.setFullName("Audit Tester");
    account.setRole(role);
    account.setStatus(UserAccountStatus.ACTIVE);
    account.setTermsAcceptedAt(Instant.now());
    createdAccounts.add(userAccountRepository.save(account));
    return "Bearer "
        + jwtTokenProvider.generateAccessToken(account.getId(), account.getEmail(), role);
  }
}
