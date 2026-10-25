package com.fa26se088.filmlink.service.lab.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.client.storage.StorageClient;
import com.fa26se088.filmlink.client.storage.StoragePurpose;
import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.lab.request.FilmLabOpeningHourRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabAcceptingOrdersRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabOpeningHoursRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabProfileRequest;
import com.fa26se088.filmlink.dto.lab.response.FilmLabImageResponse;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.entity.lab.FilmLab;
import com.fa26se088.filmlink.entity.lab.FilmLabImage;
import com.fa26se088.filmlink.entity.lab.FilmLabOpeningHour;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.mapper.lab.FilmLabMapper;
import com.fa26se088.filmlink.repository.lab.FilmLabImageRepository;
import com.fa26se088.filmlink.repository.lab.FilmLabOpeningHourRepository;
import com.fa26se088.filmlink.repository.lab.FilmLabRepository;
import com.fa26se088.filmlink.service.account.event.LabRegistrationApprovedEvent;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import com.fa26se088.filmlink.service.lab.LabAccessService;
import com.fa26se088.filmlink.service.lab.MachineReadiness;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class FilmLabServiceImplTest {

  private static final Long OWNER_ID = 7L;
  private static final Long LAB_ID = 100L;
  private static final Long APPLICATION_ID = 55L;
  private static final String DESCRIPTION =
      "Lab tráng phim màu C-41 và đen trắng, scan Noritsu, ở quận 3.";

  @Mock private FilmLabRepository filmLabRepository;
  @Mock private FilmLabOpeningHourRepository filmLabOpeningHourRepository;
  @Mock private FilmLabImageRepository filmLabImageRepository;
  @Mock private LabAccessService labAccessService;
  @Mock private MachineReadiness machineReadiness;
  @Mock private StorageClient storageClient;
  @Mock private AuditLogService auditLogService;

  private FilmLabServiceImpl service;
  private FilmLab lab;

  @BeforeEach
  void setUp() {
    service =
        new FilmLabServiceImpl(
            filmLabRepository,
            filmLabOpeningHourRepository,
            filmLabImageRepository,
            labAccessService,
            machineReadiness,
            storageClient,
            auditLogService,
            new FilmLabMapper());
    lab = new FilmLab();
    ReflectionTestUtils.setField(lab, "id", LAB_ID);
    lab.setOwnerAccountId(OWNER_ID);
    lab.setLabName("Lab Cũ");
    lenient().when(labAccessService.requireOwnedLab(OWNER_ID)).thenReturn(lab);
    lenient().when(filmLabRepository.findByIdForUpdate(LAB_ID)).thenReturn(Optional.of(lab));
    lenient()
        .when(storageClient.signedUrl(any()))
        .thenAnswer(inv -> URI.create("http://files/" + inv.getArgument(0)));
  }

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  // --- Listener: LabRegistrationApprovedEvent (BR-03) ---

  @Test
  void onLabRegistrationApproved_createsLabNotAcceptingOrders() {
    when(filmLabRepository.existsByLabRegistrationApplicationId(APPLICATION_ID)).thenReturn(false);

    service.onLabRegistrationApproved(event());

    ArgumentCaptor<FilmLab> saved = ArgumentCaptor.forClass(FilmLab.class);
    verify(filmLabRepository).saveAndFlush(saved.capture());
    FilmLab created = saved.getValue();
    assertThat(created.getOwnerAccountId()).isEqualTo(OWNER_ID);
    assertThat(created.getLabRegistrationApplicationId()).isEqualTo(APPLICATION_ID);
    assertThat(created.getLabName()).isEqualTo("Lab Mới");
    assertThat(created.getContactPhone()).isEqualTo("0901234567");
    assertThat(created.isAcceptingOrders()).isFalse();
    assertThat(created.getQcBufferHours()).isEqualTo((short) 4);
    assertThat(created.getDescription()).isNull();
  }

  @Test
  void onLabRegistrationApproved_labAlreadyExists_doesNothing() {
    when(filmLabRepository.existsByLabRegistrationApplicationId(APPLICATION_ID)).thenReturn(true);

    service.onLabRegistrationApproved(event());

    verify(filmLabRepository, never()).saveAndFlush(any());
  }

  // --- Sửa hồ sơ (UC12) ---

  @Test
  void updateMyProfile_validRequest_updatesLab() {
    var response =
        service.updateMyProfile(
            OWNER_ID,
            profileRequest(new BigDecimal("10.776889"), new BigDecimal("106.700806")));

    assertThat(lab.getLabName()).isEqualTo("Lab Mới");
    assertThat(lab.getLatitude()).isEqualByComparingTo("10.776889");
    assertThat(lab.getQcBufferHours()).isEqualTo((short) 6);
    assertThat(lab.getExpressDailyCapPercent()).isNull();
    assertThat(response.labName()).isEqualTo("Lab Mới");
  }

  @Test
  void updateMyProfile_bothCoordinatesEmpty_isAllowed() {
    service.updateMyProfile(OWNER_ID, profileRequest(null, null));

    assertThat(lab.getLatitude()).isNull();
    assertThat(lab.getLongitude()).isNull();
  }

  /** UH-06: toạ độ nhập tay phải có cả hai hoặc không có cả hai. */
  @Test
  void updateMyProfile_onlyLatitude_returnsValidationError() {
    assertError(
        () -> service.updateMyProfile(OWNER_ID, profileRequest(new BigDecimal("10.5"), null)),
        ErrorCode.VALIDATION_ERROR);
    assertThat(lab.getLabName()).isEqualTo("Lab Cũ");
  }

  @Test
  void updateMyProfile_onlyLongitude_returnsValidationError() {
    assertError(
        () -> service.updateMyProfile(OWNER_ID, profileRequest(null, new BigDecimal("106.5"))),
        ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void updateMyProfile_descriptionTooShortAfterStrip_returnsValidationError() {
    var request =
        new UpdateFilmLabProfileRequest(
            "Lab Mới",
            "  ngắn" + " ".repeat(60),
            "1 Lê Lợi",
            "Quận 1",
            "TP.HCM",
            null,
            null,
            (short) 4,
            null);

    assertError(() -> service.updateMyProfile(OWNER_ID, request), ErrorCode.VALIDATION_ERROR);
  }

  // --- Giờ mở cửa (UC12) ---

  @Test
  void updateMyOpeningHours_valid_upsertsSevenDays() {
    FilmLabOpeningHour monday = new FilmLabOpeningHour();
    monday.setFilmLabId(LAB_ID);
    monday.setDayOfWeek((short) 1);
    when(filmLabOpeningHourRepository.findByFilmLabIdOrderByDayOfWeek(LAB_ID))
        .thenReturn(List.of(monday));

    service.updateMyOpeningHours(OWNER_ID, new UpdateFilmLabOpeningHoursRequest(week()));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<FilmLabOpeningHour>> saved = ArgumentCaptor.forClass(List.class);
    verify(filmLabOpeningHourRepository).saveAllAndFlush(saved.capture());
    assertThat(saved.getValue()).hasSize(7);
    // Ngày đã có thì sửa dòng cũ, không tạo dòng mới (upsert theo ngày).
    assertThat(saved.getValue().get(0)).isSameAs(monday);
    assertThat(monday.getOpensAt()).isEqualTo(LocalTime.of(8, 0));
    FilmLabOpeningHour sunday = saved.getValue().get(6);
    assertThat(sunday.isClosed()).isTrue();
    assertThat(sunday.getOpensAt()).isNull();
    assertThat(sunday.getFilmLabId()).isEqualTo(LAB_ID);
  }

  @Test
  void updateMyOpeningHours_opensAtEqualsClosesAt_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.set(2, open(3, LocalTime.of(9, 0), LocalTime.of(9, 0)));

    assertOpeningHoursRejected(week);
  }

  @Test
  void updateMyOpeningHours_opensAfterCloses_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.set(2, open(3, LocalTime.of(18, 0), LocalTime.of(8, 0)));

    assertOpeningHoursRejected(week);
  }

  @Test
  void updateMyOpeningHours_openDayMissingTime_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.set(2, open(3, LocalTime.of(8, 0), null));

    assertOpeningHoursRejected(week);
  }

  @Test
  void updateMyOpeningHours_closedDayWithTime_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.set(6, new FilmLabOpeningHourRequest((short) 7, true, LocalTime.of(8, 0), null));

    assertOpeningHoursRejected(week);
  }

  @Test
  void updateMyOpeningHours_duplicateDay_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.set(6, open(1, LocalTime.of(8, 0), LocalTime.of(17, 0)));

    assertOpeningHoursRejected(week);
  }

  @Test
  void updateMyOpeningHours_missingDay_returnsValidationError() {
    List<FilmLabOpeningHourRequest> week = week();
    week.remove(6);

    assertOpeningHoursRejected(week);
  }

  // --- Ảnh mẫu (UC12) ---

  @Test
  void uploadMyImage_usesSmallestFreeDisplayOrder() {
    when(filmLabImageRepository.findByFilmLabIdOrderByDisplayOrder(LAB_ID))
        .thenReturn(List.of(image(1), image(3)));
    when(storageClient.upload(eq(StoragePurpose.LAB_IMAGE), any(), any(), anyLong()))
        .thenReturn("lab-image/new.png");

    FilmLabImageResponse response = service.uploadMyImage(OWNER_ID, file());

    assertThat(response.displayOrder()).isEqualTo((short) 2);
    assertThat(response.url()).hasToString("http://files/lab-image/new.png");
  }

  @Test
  void uploadMyImage_twentyFirstImage_returnsLimitReachedWithoutUploading() {
    List<FilmLabImage> twenty = IntStream.rangeClosed(1, 20).mapToObj(this::image).toList();
    when(filmLabImageRepository.findByFilmLabIdOrderByDisplayOrder(LAB_ID)).thenReturn(twenty);

    assertError(() -> service.uploadMyImage(OWNER_ID, file()), ErrorCode.LAB_IMAGE_LIMIT_REACHED);
    verify(storageClient, never()).upload(any(), any(), any(), anyLong());
  }

  @Test
  void uploadMyImage_dbSaveFails_deletesUploadedBlob() {
    when(filmLabImageRepository.findByFilmLabIdOrderByDisplayOrder(LAB_ID)).thenReturn(List.of());
    when(storageClient.upload(eq(StoragePurpose.LAB_IMAGE), any(), any(), anyLong()))
        .thenReturn("lab-image/orphan.png");
    when(filmLabImageRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uq_film_lab_images_order"));

    assertThatThrownBy(() -> service.uploadMyImage(OWNER_ID, file()))
        .isInstanceOf(DataIntegrityViolationException.class);
    verify(storageClient).delete("lab-image/orphan.png");
  }

  @Test
  void deleteMyImage_otherLabsImage_returnsNotFound() {
    when(filmLabImageRepository.findByIdAndFilmLabId(9L, LAB_ID)).thenReturn(Optional.empty());

    assertError(() -> service.deleteMyImage(OWNER_ID, 9L), ErrorCode.LAB_IMAGE_NOT_FOUND);
    verify(storageClient, never()).delete(any());
  }

  @Test
  void deleteMyImage_deletesRowNow_deletesFileOnlyAfterCommit() {
    FilmLabImage image = image(4);
    when(filmLabImageRepository.findByIdAndFilmLabId(4L, LAB_ID)).thenReturn(Optional.of(image));
    TransactionSynchronizationManager.initSynchronization();

    service.deleteMyImage(OWNER_ID, 4L);

    verify(filmLabImageRepository).delete(image);
    verify(filmLabImageRepository).flush();
    verify(storageClient, never()).delete(any());
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    verify(storageClient).delete("lab-image/4.png");
  }

  /** Commit lỗi / rollback: dòng DB còn nên file cũng phải còn. */
  @Test
  void deleteMyImage_rolledBack_keepsFile() {
    when(filmLabImageRepository.findByIdAndFilmLabId(4L, LAB_ID))
        .thenReturn(Optional.of(image(4)));
    TransactionSynchronizationManager.initSynchronization();

    service.deleteMyImage(OWNER_ID, 4L);
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    verify(storageClient, never()).delete(any());
  }

  @Test
  void deleteMyImage_storageDeleteFailsAfterCommit_onlyLogs() {
    when(filmLabImageRepository.findByIdAndFilmLabId(4L, LAB_ID))
        .thenReturn(Optional.of(image(4)));
    doThrow(new IllegalStateException("storage down")).when(storageClient).delete(any());
    TransactionSynchronizationManager.initSynchronization();

    service.deleteMyImage(OWNER_ID, 4L);

    assertThatCode(
            () ->
                TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit))
        .doesNotThrowAnyException();
    verify(storageClient).delete("lab-image/4.png");
  }

  // --- Nhận đơn (BR-57) ---

  @Test
  void updateMyAcceptingOrders_machinesNotReady_returnsMachinesNotReady() {
    when(machineReadiness.hasActiveProcessorAndScanner(LAB_ID)).thenReturn(false);

    assertError(
        () -> service.updateMyAcceptingOrders(OWNER_ID, accepting(true)),
        ErrorCode.LAB_MACHINES_NOT_READY);
    assertThat(lab.isAcceptingOrders()).isFalse();
    verify(auditLogService, never()).recordSuccess(any());
  }

  @Test
  void updateMyAcceptingOrders_machinesReady_turnsOnAndAudits() {
    when(machineReadiness.hasActiveProcessorAndScanner(LAB_ID)).thenReturn(true);

    var response = service.updateMyAcceptingOrders(OWNER_ID, accepting(true));

    assertThat(response.acceptingOrders()).isTrue();
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditLogService).recordSuccess(entry.capture());
    assertThat(entry.getValue().action()).isEqualTo(AuditAction.LAB_ACCEPTING_ORDERS_CHANGED);
    assertThat(entry.getValue().targetType()).isEqualTo(AuditTargetType.FILM_LAB);
    assertThat(entry.getValue().targetId()).isEqualTo(LAB_ID);
    assertThat(entry.getValue().before()).isEqualTo(Map.of("acceptingOrders", false));
    assertThat(entry.getValue().after()).isEqualTo(Map.of("acceptingOrders", true));
  }

  @Test
  void updateMyAcceptingOrders_turnOff_alwaysAllowedWithoutMachineCheck() {
    lab.setAcceptingOrders(true);

    var response = service.updateMyAcceptingOrders(OWNER_ID, accepting(false));

    assertThat(response.acceptingOrders()).isFalse();
    verify(machineReadiness, never()).hasActiveProcessorAndScanner(any());
    verify(auditLogService).recordSuccess(any());
  }

  @Test
  void updateMyAcceptingOrders_unchanged_noAudit() {
    service.updateMyAcceptingOrders(OWNER_ID, accepting(false));

    verify(auditLogService, never()).recordSuccess(any());
  }

  // --- helpers ---

  private void assertOpeningHoursRejected(List<FilmLabOpeningHourRequest> week) {
    assertError(
        () -> service.updateMyOpeningHours(OWNER_ID, new UpdateFilmLabOpeningHoursRequest(week)),
        ErrorCode.VALIDATION_ERROR);
    verify(filmLabOpeningHourRepository, never()).saveAllAndFlush(any());
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(expected);
  }

  private static LabRegistrationApprovedEvent event() {
    return new LabRegistrationApprovedEvent(
        APPLICATION_ID,
        OWNER_ID,
        "Lab Mới",
        "1 Lê Lợi",
        "Quận 1",
        "TP.HCM",
        null,
        null,
        "lab@example.com",
        "0901234567");
  }

  private static UpdateFilmLabProfileRequest profileRequest(
      BigDecimal latitude, BigDecimal longitude) {
    return new UpdateFilmLabProfileRequest(
        "  Lab Mới ",
        DESCRIPTION,
        "1 Lê Lợi",
        "Quận 1",
        "TP.HCM",
        latitude,
        longitude,
        (short) 6,
        null);
  }

  /** Thứ Hai–Thứ Bảy mở 08:00–18:00, Chủ nhật nghỉ. */
  private static List<FilmLabOpeningHourRequest> week() {
    List<FilmLabOpeningHourRequest> week = new ArrayList<>();
    for (int day = 1; day <= 6; day++) {
      week.add(open(day, LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }
    week.add(new FilmLabOpeningHourRequest((short) 7, true, null, null));
    return week;
  }

  private static FilmLabOpeningHourRequest open(int day, LocalTime opensAt, LocalTime closesAt) {
    return new FilmLabOpeningHourRequest((short) day, false, opensAt, closesAt);
  }

  private FilmLabImage image(int displayOrder) {
    FilmLabImage image = new FilmLabImage();
    ReflectionTestUtils.setField(image, "id", (long) displayOrder);
    image.setFilmLabId(LAB_ID);
    image.setBlobKey("lab-image/" + displayOrder + ".png");
    image.setDisplayOrder((short) displayOrder);
    return image;
  }

  private static MockMultipartFile file() {
    return new MockMultipartFile("file", "sample.png", "image/png", new byte[] {1, 2, 3});
  }

  private static UpdateFilmLabAcceptingOrdersRequest accepting(boolean value) {
    return new UpdateFilmLabAcceptingOrdersRequest(value);
  }
}
