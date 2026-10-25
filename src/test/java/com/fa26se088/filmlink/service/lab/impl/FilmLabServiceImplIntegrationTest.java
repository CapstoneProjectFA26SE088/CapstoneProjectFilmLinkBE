package com.fa26se088.filmlink.service.lab.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.client.storage.StorageClient;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.service.account.event.LabRegistrationApprovedEvent;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Listener {@code LabRegistrationApprovedEvent} trên DB thật (schema test): tạo lab trong cùng
 * transaction duyệt đơn (BR-03), idempotent, và rollback cả việc duyệt đơn khi tạo lab lỗi.
 */
@SpringBootTest
@ActiveProfiles("test")
class FilmLabServiceImplIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private StorageClient storageClient;

  private FilmLabFixture fixture;

  @BeforeEach
  void setUp() {
    fixture =
        new FilmLabFixture(
            jdbcTemplate, userAccountRepository, eventPublisher, transactionTemplate, storageClient);
  }

  @AfterEach
  void cleanUp() {
    fixture.cleanUp();
  }

  @Test
  void approvedEvent_createsLabNotAcceptingOrders() {
    UserAccount owner = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);
    long applicationId = fixture.insertPendingApplication("Lab Tích Hợp");

    fixture.approve(applicationId, owner.getId(), "Lab Tích Hợp");

    Map<String, Object> lab =
        jdbcTemplate.queryForMap(
            "SELECT owner_account_id, lab_name, is_accepting_orders, qc_buffer_hours, description"
                + " FROM film_labs WHERE lab_registration_application_id = ?",
            applicationId);
    assertThat(lab.get("owner_account_id")).isEqualTo(owner.getId());
    assertThat(lab.get("lab_name")).isEqualTo("Lab Tích Hợp");
    assertThat(lab.get("is_accepting_orders")).isEqualTo(false);
    assertThat(((Number) lab.get("qc_buffer_hours")).intValue()).isEqualTo(4);
    assertThat(lab.get("description")).isNull();
    assertThat(fixture.applicationStatus(applicationId)).isEqualTo("APPROVED");
  }

  @Test
  void approvedEventPublishedTwice_createsOneLab() {
    UserAccount owner = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);
    long applicationId = fixture.insertPendingApplication("Lab Hai Lần");

    fixture.approve(applicationId, owner.getId(), "Lab Hai Lần");
    fixture.approve(applicationId, owner.getId(), "Lab Hai Lần");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM film_labs WHERE lab_registration_application_id = ?",
                Integer.class,
                applicationId))
        .isEqualTo(1);
  }

  /** Listener {@code MANDATORY}: publish ngoài transaction thì lỗi và không tạo lab. */
  @Test
  void publishedOutsideTransaction_failsWithoutCreatingLab() {
    UserAccount owner = fixture.createAccount(UserAccountRole.FILM_LAB_OWNER);
    long applicationId = fixture.insertPendingApplication("Lab Ngoài Transaction");

    assertThatThrownBy(
            () ->
                eventPublisher.publishEvent(
                    new LabRegistrationApprovedEvent(
                        applicationId,
                        owner.getId(),
                        "Lab Ngoài Transaction",
                        "12 Nguyễn Huệ",
                        "Quận 1",
                        "TP. Hồ Chí Minh",
                        null,
                        null,
                        "lab@example.com",
                        "0901234567")))
        .isInstanceOf(IllegalTransactionStateException.class);

    assertThat(fixture.labIdOfApplication(applicationId)).isNull();
  }

  /** Trigger {@code film_labs_owner_role} chặn owner không phải FILM_LAB_OWNER: duyệt đơn rollback. */
  @Test
  void ownerWithoutFilmLabOwnerRole_rollsBackApproval() {
    UserAccount photographer = fixture.createAccount(UserAccountRole.PHOTOGRAPHER);
    long applicationId = fixture.insertPendingApplication("Lab Sai Role");

    assertThatThrownBy(() -> fixture.approve(applicationId, photographer.getId(), "Lab Sai Role"))
        .isInstanceOf(DataAccessException.class);

    assertThat(fixture.labIdOfApplication(applicationId)).isNull();
    assertThat(fixture.applicationStatus(applicationId)).isEqualTo("PENDING_REVIEW");
  }
}
