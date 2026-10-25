package com.fa26se088.filmlink.service.lab.impl;

import com.fa26se088.filmlink.client.storage.StorageClient;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.service.account.event.LabRegistrationApprovedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu thử cho integration test của domain lab trên DB thật (schema test).
 *
 * <p>Đơn đăng ký lab và máy chèn bằng {@link JdbcTemplate} vì chưa có entity (REG-01, MCH-01). Lab
 * được tạo qua đúng đường thật: publish {@link LabRegistrationApprovedEvent} trong một transaction,
 * như REG-02 sẽ làm.
 *
 * <p>{@link #cleanUp()} xoá máy, ảnh (kể cả file), lab, đơn đăng ký và tài khoản đã tạo. Tài khoản
 * đã có dòng audit log thì không xoá được (FK từ {@code audit_logs}) nên được giữ lại; email ngẫu
 * nhiên nên không ảnh hưởng lần chạy sau.
 */
public class FilmLabFixture {

  private final JdbcTemplate jdbcTemplate;
  private final UserAccountRepository userAccountRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;
  private final StorageClient storageClient;

  private final List<Long> accountIds = new ArrayList<>();
  private final List<Long> applicationIds = new ArrayList<>();

  public FilmLabFixture(
      JdbcTemplate jdbcTemplate,
      UserAccountRepository userAccountRepository,
      ApplicationEventPublisher eventPublisher,
      TransactionTemplate transactionTemplate,
      StorageClient storageClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.userAccountRepository = userAccountRepository;
    this.eventPublisher = eventPublisher;
    this.transactionTemplate = transactionTemplate;
    this.storageClient = storageClient;
  }

  public UserAccount createAccount(UserAccountRole role) {
    UserAccount account = new UserAccount();
    account.setEmail("lab-it-" + UUID.randomUUID() + "@example.com");
    account.setPasswordHash("not-a-real-hash");
    account.setFullName("Lab Tester");
    account.setRole(role);
    account.setStatus(UserAccountStatus.ACTIVE);
    account.setTermsAcceptedAt(Instant.now());
    userAccountRepository.save(account);
    accountIds.add(account.getId());
    return account;
  }

  /** Đơn đăng ký ở trạng thái PENDING_REVIEW. */
  public long insertPendingApplication(String labName) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO lab_registration_applications
                (application_code, lab_name, tax_code, address_line, district, province,
                 contact_email, contact_phone)
            VALUES (?, ?, ?, '12 Nguyễn Huệ', 'Quận 1', 'TP. Hồ Chí Minh', 'lab@example.com',
                    '0901234567')
            RETURNING id
            """,
            Long.class,
            "IT-" + UUID.randomUUID().toString().substring(0, 12),
            labName,
            String.valueOf(random.nextLong(1_000_000_000L, 10_000_000_000L)));
    applicationIds.add(id);
    return id;
  }

  /**
   * Giả lập REG-02: trong một transaction, đổi đơn sang APPROVED rồi publish event. Listener tạo
   * lab lỗi thì exception bay ra và cả transaction rollback.
   */
  public void approve(long applicationId, long ownerAccountId, String labName) {
    transactionTemplate.executeWithoutResult(
        status -> {
          jdbcTemplate.update(
              "UPDATE lab_registration_applications SET status = 'APPROVED', reviewed_at = now()"
                  + " WHERE id = ?",
              applicationId);
          eventPublisher.publishEvent(
              new LabRegistrationApprovedEvent(
                  applicationId,
                  ownerAccountId,
                  labName,
                  "12 Nguyễn Huệ",
                  "Quận 1",
                  "TP. Hồ Chí Minh",
                  null,
                  null,
                  "lab@example.com",
                  "0901234567"));
        });
  }

  /** Tạo tài khoản FILM_LAB_OWNER kèm lab đã duyệt; trả id lab. */
  public long createLab(UserAccount owner, String labName) {
    long applicationId = insertPendingApplication(labName);
    approve(applicationId, owner.getId(), labName);
    return labIdOfApplication(applicationId);
  }

  public Long labIdOfApplication(long applicationId) {
    return jdbcTemplate.query(
            "SELECT id FROM film_labs WHERE lab_registration_application_id = ?",
            (rs, row) -> rs.getLong("id"),
            applicationId)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public String applicationStatus(long applicationId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM lab_registration_applications WHERE id = ?",
        String.class,
        applicationId);
  }

  public void insertMachine(long labId, String machineType, String status) {
    jdbcTemplate.update(
        "INSERT INTO machines (film_lab_id, machine_code, machine_type, capacity_per_hour, status)"
            + " VALUES (?, ?, ?, 26, ?)",
        labId,
        "IT-" + UUID.randomUUID().toString().substring(0, 8),
        machineType,
        status);
  }

  public void cleanUp() {
    for (Long applicationId : applicationIds) {
      Long labId = labIdOfApplication(applicationId);
      if (labId != null) {
        jdbcTemplate
            .queryForList(
                "SELECT blob_key FROM film_lab_images WHERE film_lab_id = ?", String.class, labId)
            .forEach(storageClient::delete);
        jdbcTemplate.update("DELETE FROM machines WHERE film_lab_id = ?", labId);
        // film_lab_opening_hours, film_lab_images: ON DELETE CASCADE.
        jdbcTemplate.update("DELETE FROM film_labs WHERE id = ?", labId);
      }
      jdbcTemplate.update("DELETE FROM lab_registration_applications WHERE id = ?", applicationId);
    }
    for (Long accountId : accountIds) {
      jdbcTemplate.update(
          "DELETE FROM user_accounts WHERE id = ?"
              + " AND NOT EXISTS (SELECT 1 FROM audit_logs WHERE actor_account_id = ?)",
          accountId,
          accountId);
    }
    applicationIds.clear();
    accountIds.clear();
  }
}
