package com.fa26se088.filmlink.service.account.event;

/**
 * Sự kiện phát sinh sau khi tài khoản đổi mật khẩu thành công (UC02).
 *
 * <p>Theo quy ước kiến trúc (mục 5 & bảng 8.1), các domain khác không gọi trực tiếp {@code
 * EmailClient} mà publish ApplicationEvent. {@code NotificationEventListener} ở domain {@code
 * notification} sẽ lắng nghe sự kiện này và gửi email cảnh báo bảo mật tới người dùng.
 *
 * @param accountId ID tài khoản vừa đổi mật khẩu
 * @param email địa chỉ email của tài khoản
 * @param fullName họ tên của người dùng
 */
public record UserAccountPasswordChangedEvent(
    Long accountId,
    String email,
    String fullName) {}
