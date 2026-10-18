package com.fa26se088.filmlink.service.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.entity.audit.AuditLog;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditOutcome;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.repository.audit.AuditLogRepository;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * BR-32 trên DB thật (schema test): lưu đủ cột (kể cả jsonb, inet), hành vi transaction của
 * recordSuccess / recordFailure, và trigger append-only.
 *
 * <p>Dòng audit không xoá được (trigger chặn dòng chưa đủ 12 tháng), nên mỗi test dùng {@code
 * targetId} ngẫu nhiên để không đụng dữ liệu của lần chạy trước. Tài khoản actor có email cố định,
 * tạo một lần rồi dùng lại (không xoá được vì audit log tham chiếu tới nó).
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditLogServiceImplIntegrationTest {

  private static final String ACTOR_EMAIL = "audit-it-actor@example.com";

  @Autowired private AuditLogService auditLogService;
  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;

  private final long targetId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);

  @AfterEach
  void clearContext() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  @Test
  void recordSuccessStoresAllColumnsIncludingJsonbAndInet() {
    AuthenticatedAccount actor = signInActor();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.7");
    // Không được tin header này: client tự gửi được.
    request.addHeader("X-Forwarded-For", "198.51.100.99");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    Instant before = Instant.now();

    auditLogService.recordSuccess(
        new AuditEntry(
            AuditAction.ACCOUNT_PASSWORD_CHANGED,
            AuditTargetType.USER_ACCOUNT,
            targetId,
            "Đổi mật khẩu",
            Map.of("status", "ACTIVE", "count", 1),
            Map.of("status", "LOCKED", "tags", List.of("a", "b"))));

    AuditLog saved = singleRow();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getOccurredAt()).isAfterOrEqualTo(before.minusSeconds(1));
    assertThat(saved.getActorAccountId()).isEqualTo(actor.id());
    assertThat(saved.getActorRole()).isEqualTo(UserAccountRole.PHOTOGRAPHER);
    assertThat(saved.getAction()).isEqualTo(AuditAction.ACCOUNT_PASSWORD_CHANGED);
    assertThat(saved.getTargetType()).isEqualTo(AuditTargetType.USER_ACCOUNT);
    assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    assertThat(saved.getReason()).isEqualTo("Đổi mật khẩu");
    assertThat(saved.getBeforeData()).containsEntry("status", "ACTIVE").containsEntry("count", 1);
    assertThat(saved.getAfterData())
        .containsEntry("status", "LOCKED")
        .containsEntry("tags", List.of("a", "b"));
    assertThat(saved.getClientIp().getHostAddress()).isEqualTo("203.0.113.7");

    // Kiểm giá trị thật trong DB, không qua Hibernate.
    Map<String, Object> raw =
        jdbcTemplate.queryForMap(
            "SELECT client_ip::text AS ip, before_data->>'status' AS before_status,"
                + " jsonb_typeof(after_data->'tags') AS tags_type, actor_role"
                + " FROM audit_logs WHERE id = ?",
            saved.getId());
    assertThat(raw)
        .containsEntry("ip", "203.0.113.7/32")
        .containsEntry("before_status", "ACTIVE")
        .containsEntry("tags_type", "array")
        .containsEntry("actor_role", "PHOTOGRAPHER");
  }

  /**
   * Không có SecurityContext = System: actor_account_id, actor_role null; ngoài HTTP request thì
   * client_ip null.
   */
  @Test
  void systemActorOutsideHttpRequestStoresNulls() {
    // Spring Test tự gắn một MockHttpServletRequest cho mỗi test; gỡ ra để giống Scheduler.
    RequestContextHolder.resetRequestAttributes();
    auditLogService.recordSuccess(entry());

    AuditLog saved = singleRow();
    assertThat(saved.getActorAccountId()).isNull();
    assertThat(saved.getActorRole()).isNull();
    assertThat(saved.getClientIp()).isNull();
    assertThat(saved.getBeforeData()).isNull();
    assertThat(saved.getAfterData()).isNull();
  }

  /** BR-32: nghiệp vụ rollback thì dòng SUCCESS cũng mất. */
  @Test
  void recordSuccessRollsBackWithOuterTransaction() {
    transactionTemplate.executeWithoutResult(
        status -> {
          auditLogService.recordSuccess(entry());
          status.setRollbackOnly();
        });

    assertThat(rows()).isEmpty();
  }

  /** BR-32: dòng FAILURE vẫn được lưu dù transaction nghiệp vụ rollback. */
  @Test
  void recordFailureSurvivesOuterRollback() {
    assertThatThrownBy(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      auditLogService.recordFailure(entry());
                      throw new IllegalStateException("nghiệp vụ thất bại");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(rows()).extracting(AuditLog::getOutcome).containsExactly(AuditOutcome.FAILURE);
  }

  /** BR-32: append-only — trigger từ chối UPDATE và DELETE dòng chưa đủ 12 tháng. */
  @Test
  void databaseRejectsUpdateAndDelete() {
    auditLogService.recordSuccess(entry());
    Long id = singleRow().getId();

    assertThatThrownBy(
            () -> jdbcTemplate.update("UPDATE audit_logs SET reason = 'sửa' WHERE id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("append-only");
    assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_logs WHERE id = ?", id))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("cannot be deleted");

    assertThat(singleRow().getReason()).isNull();
  }

  private AuditEntry entry() {
    return new AuditEntry(
        AuditAction.ACCOUNT_PASSWORD_CHANGED,
        AuditTargetType.USER_ACCOUNT,
        targetId,
        null,
        null,
        null);
  }

  private List<AuditLog> rows() {
    return auditLogRepository.findAll(
        AuditLogRepository.byTarget(AuditTargetType.USER_ACCOUNT, targetId));
  }

  private AuditLog singleRow() {
    List<AuditLog> rows = rows();
    assertThat(rows).hasSize(1);
    return rows.getFirst();
  }

  /** Đặt tài khoản actor vào SecurityContext như JwtAuthenticationFilter làm; clear ở @AfterEach. */
  private AuthenticatedAccount signInActor() {
    UserAccount account =
        userAccountRepository
            .findByEmail(ACTOR_EMAIL)
            .orElseGet(
                () -> {
                  UserAccount created = new UserAccount();
                  created.setEmail(ACTOR_EMAIL);
                  created.setPasswordHash("not-a-real-hash");
                  created.setFullName("Audit Actor");
                  created.setRole(UserAccountRole.PHOTOGRAPHER);
                  created.setStatus(UserAccountStatus.ACTIVE);
                  created.setTermsAcceptedAt(Instant.now());
                  return userAccountRepository.save(created);
                });
    AuthenticatedAccount actor =
        new AuthenticatedAccount(account.getId(), account.getEmail(), account.getRole());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(actor, null, actor.authorities()));
    return actor;
  }
}
