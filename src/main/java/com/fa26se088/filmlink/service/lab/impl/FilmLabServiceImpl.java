package com.fa26se088.filmlink.service.lab.impl;

import com.fa26se088.filmlink.client.storage.StorageClient;
import com.fa26se088.filmlink.client.storage.StoragePurpose;
import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.lab.request.FilmLabOpeningHourRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabAcceptingOrdersRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabOpeningHoursRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabProfileRequest;
import com.fa26se088.filmlink.dto.lab.response.FilmLabImageResponse;
import com.fa26se088.filmlink.dto.lab.response.FilmLabProfileResponse;
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
import com.fa26se088.filmlink.service.lab.FilmLabService;
import com.fa26se088.filmlink.service.lab.LabAccessService;
import com.fa26se088.filmlink.service.lab.MachineReadiness;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmLabServiceImpl implements FilmLabService {

  /** UC12: tối đa 20 ảnh mẫu mỗi lab ({@code ck_film_lab_images_order}: 1..20). */
  static final int MAX_IMAGES = 20;

  private static final int DAYS_PER_WEEK = 7;
  private static final String ACCEPTING_ORDERS = "acceptingOrders";

  private final FilmLabRepository filmLabRepository;
  private final FilmLabOpeningHourRepository filmLabOpeningHourRepository;
  private final FilmLabImageRepository filmLabImageRepository;
  private final LabAccessService labAccessService;
  private final MachineReadiness machineReadiness;
  private final StorageClient storageClient;
  private final AuditLogService auditLogService;
  private final FilmLabMapper filmLabMapper;

  /**
   * BR-03: đơn đăng ký được duyệt thì tạo Film Lab, chưa nhận đơn ({@code is_accepting_orders =
   * false}, BR-34), các cột còn lại lấy {@code DEFAULT} của DB; chưa có giờ mở cửa hay ảnh.
   *
   * <p>Đồng bộ, chạy trong transaction duyệt đơn của REG-02 (không dùng {@code
   * TransactionalEventListener}): tạo lab lỗi, ví dụ trigger {@code film_labs_owner_role} từ chối
   * owner chưa có role FILM_LAB_OWNER, thì việc duyệt đơn cũng rollback. {@code MANDATORY}: publish
   * ngoài transaction thì ném {@code IllegalTransactionStateException}, không tạo lab rời khỏi việc
   * duyệt đơn.
   *
   * <p>Idempotent: đơn đã có lab thì chỉ ghi log, không tạo thêm.
   */
  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void onLabRegistrationApproved(LabRegistrationApprovedEvent event) {
    if (filmLabRepository.existsByLabRegistrationApplicationId(
        event.labRegistrationApplicationId())) {
      log.info(
          "Film Lab for lab registration application {} already exists, skipping",
          event.labRegistrationApplicationId());
      return;
    }

    FilmLab lab = new FilmLab();
    lab.setOwnerAccountId(event.ownerAccountId());
    lab.setLabRegistrationApplicationId(event.labRegistrationApplicationId());
    lab.setLabName(event.labName());
    lab.setAddressLine(event.addressLine());
    lab.setDistrict(event.district());
    lab.setProvince(event.province());
    lab.setLatitude(event.latitude());
    lab.setLongitude(event.longitude());
    lab.setContactEmail(event.contactEmail());
    lab.setContactPhone(event.contactPhone());
    filmLabRepository.saveAndFlush(lab);
    log.info(
        "Created Film Lab {} for lab registration application {}",
        lab.getId(),
        event.labRegistrationApplicationId());
  }

  @Override
  @Transactional(readOnly = true)
  public FilmLabProfileResponse getMyProfile(Long ownerAccountId) {
    return toProfileResponse(labAccessService.requireOwnedLab(ownerAccountId));
  }

  /**
   * UC12: tên 2–150, mô tả 50–2000 ký tự (đếm sau khi bỏ khoảng trắng hai đầu). UH-06: toạ độ nhập
   * tay, phải có cả hai hoặc trống cả hai. BR-60: {@code qcBufferHours} 0–48. BR-61: {@code
   * expressDailyCapPercent} 1–100 hoặc {@code null} (không giới hạn).
   */
  @Override
  @Transactional
  public FilmLabProfileResponse updateMyProfile(
      Long ownerAccountId, UpdateFilmLabProfileRequest request) {
    if ((request.latitude() == null) != (request.longitude() == null)) {
      throw validationError("Vĩ độ và kinh độ phải có cả hai hoặc để trống cả hai.");
    }
    FilmLab lab = labAccessService.requireOwnedLab(ownerAccountId);
    lab.setLabName(stripWithLength(request.labName(), 2, 150, "Tên lab"));
    lab.setDescription(stripWithLength(request.description(), 50, 2000, "Mô tả"));
    lab.setAddressLine(request.addressLine().strip());
    lab.setDistrict(request.district().strip());
    lab.setProvince(request.province().strip());
    lab.setLatitude(request.latitude());
    lab.setLongitude(request.longitude());
    lab.setQcBufferHours(request.qcBufferHours());
    lab.setExpressDailyCapPercent(request.expressDailyCapPercent());
    filmLabRepository.flush();
    return toProfileResponse(lab);
  }

  /**
   * UC12: giờ mở cửa đủ 7 ngày (ISO 1–7), không trùng ngày. Ngày nghỉ không có giờ; ngày mở có cả
   * giờ mở và giờ đóng, giờ mở trước giờ đóng. Upsert theo {@code (film_lab_id, day_of_week)}.
   */
  @Override
  @Transactional
  public FilmLabProfileResponse updateMyOpeningHours(
      Long ownerAccountId, UpdateFilmLabOpeningHoursRequest request) {
    validateOpeningHours(request.openingHours());
    FilmLab lab = lockOwnedLab(ownerAccountId);

    Map<Short, FilmLabOpeningHour> existing =
        filmLabOpeningHourRepository.findByFilmLabIdOrderByDayOfWeek(lab.getId()).stream()
            .collect(Collectors.toMap(FilmLabOpeningHour::getDayOfWeek, Function.identity()));
    List<FilmLabOpeningHour> hours = new ArrayList<>(DAYS_PER_WEEK);
    for (FilmLabOpeningHourRequest item : request.openingHours()) {
      FilmLabOpeningHour hour =
          existing.computeIfAbsent(item.dayOfWeek(), day -> newOpeningHour(lab.getId(), day));
      boolean closed = item.closed();
      hour.setClosed(closed);
      hour.setOpensAt(closed ? null : item.opensAt());
      hour.setClosesAt(closed ? null : item.closesAt());
      hours.add(hour);
    }
    filmLabOpeningHourRepository.saveAllAndFlush(hours);
    return toProfileResponse(lab);
  }

  /**
   * UC12: tối đa 20 ảnh, JPG/PNG ≤ 10 MB ({@link StoragePurpose#LAB_IMAGE} kiểm). {@code
   * display_order} là số nhỏ nhất còn trống trong 1..20. Upload file trước, lưu DB sau; lưu DB lỗi
   * thì xoá file vừa upload. UH-39: storage lỗi chỉ ảnh này không lưu, hồ sơ không bị ảnh hưởng.
   */
  @Override
  @Transactional
  public FilmLabImageResponse uploadMyImage(Long ownerAccountId, MultipartFile file) {
    FilmLab lab = lockOwnedLab(ownerAccountId);
    Set<Short> usedOrders =
        filmLabImageRepository.findByFilmLabIdOrderByDisplayOrder(lab.getId()).stream()
            .map(FilmLabImage::getDisplayOrder)
            .collect(Collectors.toSet());
    if (usedOrders.size() >= MAX_IMAGES) {
      throw new BusinessException(
          ErrorCode.LAB_IMAGE_LIMIT_REACHED,
          "Mỗi Film Lab có tối đa " + MAX_IMAGES + " ảnh mẫu.",
          Map.of("limit", MAX_IMAGES));
    }
    short displayOrder = smallestFreeOrder(usedOrders);

    String blobKey = uploadToStorage(file);
    FilmLabImage image = new FilmLabImage();
    image.setFilmLabId(lab.getId());
    image.setBlobKey(blobKey);
    image.setDisplayOrder(displayOrder);
    try {
      filmLabImageRepository.saveAndFlush(image);
    } catch (RuntimeException ex) {
      deleteBlobQuietly(blobKey);
      throw ex;
    }
    return filmLabMapper.toImageResponse(image, storageClient.signedUrl(blobKey));
  }

  /**
   * UC12: xoá dòng DB trước, xoá file sau. File chỉ bị xoá khi transaction đã commit ({@code
   * afterCommit}): commit lỗi thì dòng DB vẫn còn, nên file nó trỏ tới cũng phải còn. Xoá file lỗi
   * chỉ ghi log: file mồ côi chấp nhận được, còn dòng DB trỏ tới file đã mất thì không. Không đánh
   * lại {@code display_order} của các ảnh còn lại.
   */
  @Override
  @Transactional
  public void deleteMyImage(Long ownerAccountId, Long imageId) {
    FilmLab lab = labAccessService.requireOwnedLab(ownerAccountId);
    FilmLabImage image =
        filmLabImageRepository
            .findByIdAndFilmLabId(imageId, lab.getId())
            .orElseThrow(
                () ->
                    new BusinessException(ErrorCode.LAB_IMAGE_NOT_FOUND, "Không tìm thấy ảnh."));
    filmLabImageRepository.delete(image);
    filmLabImageRepository.flush();
    String blobKey = image.getBlobKey();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            deleteBlobQuietly(blobKey);
          }
        });
  }

  /**
   * BR-57: chỉ bật nhận đơn khi lab có ít nhất một máy PROCESSOR và một máy SCANNER đang ACTIVE.
   * Tắt luôn được; đơn đang xử lý vẫn chạy tiếp (UC12). BR-32: ghi audit log khi trạng thái thay
   * đổi; gửi lại đúng trạng thái hiện tại thì không làm gì.
   */
  @Override
  @Transactional
  public FilmLabProfileResponse updateMyAcceptingOrders(
      Long ownerAccountId, UpdateFilmLabAcceptingOrdersRequest request) {
    FilmLab lab = labAccessService.requireOwnedLab(ownerAccountId);
    boolean before = lab.isAcceptingOrders();
    boolean after = request.acceptingOrders();
    if (before == after) {
      return toProfileResponse(lab);
    }
    if (after && !machineReadiness.hasActiveProcessorAndScanner(lab.getId())) {
      throw new BusinessException(
          ErrorCode.LAB_MACHINES_NOT_READY,
          "Cần khai báo ít nhất một máy tráng (PROCESSOR) và một máy scan (SCANNER) đang hoạt động"
              + " trước khi nhận đơn.");
    }

    lab.setAcceptingOrders(after);
    filmLabRepository.flush();
    auditLogService.recordSuccess(
        new AuditEntry(
            AuditAction.LAB_ACCEPTING_ORDERS_CHANGED,
            AuditTargetType.FILM_LAB,
            lab.getId(),
            null,
            Map.of(ACCEPTING_ORDERS, before),
            Map.of(ACCEPTING_ORDERS, after)));
    return toProfileResponse(lab);
  }

  private FilmLabProfileResponse toProfileResponse(FilmLab lab) {
    List<FilmLabImageResponse> images =
        filmLabImageRepository.findByFilmLabIdOrderByDisplayOrder(lab.getId()).stream()
            .map(
                image ->
                    filmLabMapper.toImageResponse(image, storageClient.signedUrl(image.getBlobKey())))
            .toList();
    return filmLabMapper.toProfileResponse(
        lab, filmLabOpeningHourRepository.findByFilmLabIdOrderByDayOfWeek(lab.getId()), images);
  }

  /** Khoá dòng lab để tuần tự hoá các thao tác sửa ảnh / giờ mở cửa của cùng một lab. */
  private FilmLab lockOwnedLab(Long ownerAccountId) {
    Long labId = labAccessService.requireOwnedLab(ownerAccountId).getId();
    return filmLabRepository
        .findByIdForUpdate(labId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.LAB_NOT_FOUND, "Không tìm thấy Film Lab."));
  }

  private static void validateOpeningHours(List<FilmLabOpeningHourRequest> openingHours) {
    if (openingHours == null || openingHours.size() != DAYS_PER_WEEK) {
      throw validationError("Giờ mở cửa phải có đủ 7 ngày trong tuần.");
    }
    Set<Short> days = new HashSet<>();
    for (FilmLabOpeningHourRequest item : openingHours) {
      if (item.dayOfWeek() == null || item.dayOfWeek() < 1 || item.dayOfWeek() > DAYS_PER_WEEK) {
        throw validationError("Ngày trong tuần phải từ 1 (Thứ Hai) đến 7 (Chủ nhật).");
      }
      if (!days.add(item.dayOfWeek())) {
        throw validationError("Ngày " + item.dayOfWeek() + " bị khai báo trùng.");
      }
      if (Boolean.TRUE.equals(item.closed())) {
        if (item.opensAt() != null || item.closesAt() != null) {
          throw validationError("Ngày " + item.dayOfWeek() + " là ngày nghỉ, không nhập giờ.");
        }
      } else if (item.opensAt() == null || item.closesAt() == null) {
        throw validationError("Ngày " + item.dayOfWeek() + " phải có cả giờ mở và giờ đóng cửa.");
      } else if (!item.opensAt().isBefore(item.closesAt())) {
        throw validationError("Ngày " + item.dayOfWeek() + ": giờ mở cửa phải trước giờ đóng cửa.");
      }
    }
  }

  private static FilmLabOpeningHour newOpeningHour(Long filmLabId, Short dayOfWeek) {
    FilmLabOpeningHour hour = new FilmLabOpeningHour();
    hour.setFilmLabId(filmLabId);
    hour.setDayOfWeek(dayOfWeek);
    return hour;
  }

  /** Số nhỏ nhất trong 1..20 chưa có ảnh nào dùng; người gọi đã kiểm còn chỗ trống. */
  private static short smallestFreeOrder(Set<Short> usedOrders) {
    for (short order = 1; order <= MAX_IMAGES; order++) {
      if (!usedOrders.contains(order)) {
        return order;
      }
    }
    throw new IllegalStateException("No free display order");
  }

  private String uploadToStorage(MultipartFile file) {
    try (InputStream content = file.getInputStream()) {
      return storageClient.upload(
          StoragePurpose.LAB_IMAGE, file.getOriginalFilename(), content, file.getSize());
    } catch (IOException ex) {
      throw validationError("Không đọc được file ảnh gửi lên.");
    }
  }

  private void deleteBlobQuietly(String blobKey) {
    try {
      storageClient.delete(blobKey);
    } catch (RuntimeException ex) {
      log.warn("Could not delete lab image blob {}", blobKey, ex);
    }
  }

  /** Bỏ khoảng trắng hai đầu rồi kiểm độ dài theo ký tự, khớp {@code char_length} của CHECK. */
  private static String stripWithLength(String value, int min, int max, String fieldName) {
    String stripped = value.strip();
    int length = stripped.codePointCount(0, stripped.length());
    if (length < min || length > max) {
      throw validationError(fieldName + " phải có " + min + "–" + max + " ký tự.");
    }
    return stripped;
  }

  private static BusinessException validationError(String message) {
    return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
  }
}
