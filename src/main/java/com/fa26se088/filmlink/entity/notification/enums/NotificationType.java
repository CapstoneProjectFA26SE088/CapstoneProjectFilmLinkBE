package com.fa26se088.filmlink.entity.notification.enums;

/**
 * Phân loại thông báo hệ thống (UC04).
 *
 * <p>Theo quy ước kiến trúc (bảng 8.1), enum của domain notification nằm trong {@code
 * entity/notification/enums}. Giá trị UPPER_SNAKE_CASE đại diện cho các nhóm thông báo chính
 * trong nền tảng FilmLink.
 */
public enum NotificationType {

  /**
   * Cảnh báo bảo mật tài khoản (UC02, NFR-SEC): đổi mật khẩu, khoá tài khoản, phiên đăng nhập lạ.
   */
  SECURITY,

  /**
   * Cập nhật tiến độ đơn dịch vụ tráng / scan (UC20, UC22, UC24, UC26, UC27, UC29, UC60, BR-07, BR-08):
   * đơn được nhận, chuyển công đoạn, đã upload preview scan, trả phim.
   */
  ORDER_STATUS,

  /**
   * Thông báo thanh toán (UC30, UC31, UC35, UC62, UC64, BR-17, BR-18, BR-31):
   * đặt cọc thành công, thanh toán chênh lệch, hoàn tiền, payout thành công.
   */
  PAYMENT,

  /**
   * Nhắc nhở công nợ hoa hồng của Film Lab (UC32, UC34, BR-20, BR-21, BR-22):
   * chốt kỳ công nợ, nhắc nợ ngày 1/5/8, cảnh báo chế tài quá hạn.
   */
  COMMISSION_DEBT,

  /**
   * Thông báo sàn ký gửi thiết bị (UC45, UC46, UC47, UC48, UC62, UC63, UC65, UC66, BR-49, BR-52, BR-53, BR-56):
   * biên bản thẩm định, thương lượng giá, tin đăng được duyệt, có người mua, hết hạn ký gửi.
   */
  MARKETPLACE,

  /**
   * Thông báo kiểm duyệt và khiếu nại (UC50, UC52, BR-15, BR-32):
   * kết quả xử lý báo cáo vi phạm, phán quyết khiếu nại của Admin/Moderator.
   */
  MODERATION,

  /**
   * Thông báo tương tác cộng đồng (UC37, UC38, UC39, UC41, UC44):
   * đánh giá lab mới, bình luận bài viết, duyệt bài Knowledge Base.
   */
  COMMUNITY,

  /**
   * Thông báo chung từ hệ thống FilmLink (bảo trì, cập nhật điều khoản hệ thống).
   */
  SYSTEM
}
