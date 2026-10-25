package com.fa26se088.filmlink.service.account.event;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Đơn đăng ký lab đã được Admin duyệt (UC08, BR-03). Domain {@code lab} lắng nghe để tạo hồ sơ
 * {@code film_labs} (UC12); {@code account} ở tầng 0 nên không gọi thẳng service của {@code lab}.
 *
 * <p><b>Cách publish (REG-02):</b> gọi {@code ApplicationEventPublisher.publishEvent(event)}
 * <b>bên trong transaction duyệt đơn</b>, sau khi đã đổi đơn sang APPROVED và tạo tài khoản
 * FILM_LAB_OWNER. Listener chạy đồng bộ trong cùng transaction: tạo lab lỗi thì cả việc duyệt đơn
 * rollback, không bao giờ có đơn APPROVED mà thiếu lab. Listener khai báo {@code propagation =
 * MANDATORY}: publish <b>ngoài</b> transaction sẽ lỗi {@code IllegalTransactionStateException} và
 * không tạo lab.
 *
 * @param labRegistrationApplicationId id đơn đăng ký đã duyệt
 * @param ownerAccountId tài khoản FILM_LAB_OWNER mà REG-02 tạo ra (trigger {@code
 *     film_labs_owner_role} từ chối role khác)
 * @param labName tên lab, copy từ đơn
 * @param addressLine địa chỉ, copy từ đơn
 * @param district quận/huyện, copy từ đơn
 * @param province tỉnh/thành, copy từ đơn
 * @param latitude vĩ độ, có thể {@code null} (UH-06); null cùng lúc với {@code longitude}
 * @param longitude kinh độ, có thể {@code null} (UH-06)
 * @param contactEmail email liên hệ, copy từ đơn
 * @param contactPhone số điện thoại liên hệ, copy từ đơn
 */
public record LabRegistrationApprovedEvent(
    Long labRegistrationApplicationId,
    Long ownerAccountId,
    String labName,
    String addressLine,
    String district,
    String province,
    BigDecimal latitude,
    BigDecimal longitude,
    String contactEmail,
    String contactPhone) {

  public LabRegistrationApprovedEvent {
    Objects.requireNonNull(labRegistrationApplicationId, "labRegistrationApplicationId");
    Objects.requireNonNull(ownerAccountId, "ownerAccountId");
  }
}
