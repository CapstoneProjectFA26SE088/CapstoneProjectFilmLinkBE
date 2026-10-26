package com.fa26se088.filmlink.entity.masterdata.enums;

import lombok.Getter;

/**
 * Danh sách định danh các tham số hệ thống (ERD #52, UC54, BR-38).
 * Quản lý có kiểu, không dùng chuỗi rải rác.
 */
@Getter
public enum SystemParameterKey {

  /** Tỉ lệ hoa hồng mặc định của sàn, 0-30% (BR-10). */
  DEFAULT_COMMISSION_RATE(ParameterValueType.PERCENT),

  /** Phí phạt khi Photographer hủy sau khi lab nhận phim, % trên số tiền đã thanh toán (BR-38). */
  ORDER_CANCELLATION_FEE_RATE(ParameterValueType.PERCENT),

  /** Ngày đến hạn thanh toán nợ hoa hồng trong tháng tiếp theo, 1-28 (BR-20). */
  BILLING_DUE_DAY_OF_MONTH(ParameterValueType.INTEGER),

  /** Số ngày quá hạn: Lab bị loại khỏi gợi ý (BR-21). */
  SANCTION_LEVEL_1_DAYS(ParameterValueType.INTEGER),

  /** Số ngày quá hạn: Lab bị ẩn khỏi kết quả tìm kiếm (BR-21). */
  SANCTION_LEVEL_2_DAYS(ParameterValueType.INTEGER),

  /** Số ngày quá hạn: Lab bị chặn nhận đơn hàng mới (BR-21). */
  SANCTION_LEVEL_3_DAYS(ParameterValueType.INTEGER),

  /** Số ngày tự động hoàn tất đơn sau khi trả hàng nếu không khiếu nại (BR-24). */
  ORDER_AUTO_CLOSE_DAYS(ParameterValueType.INTEGER),

  /** Số ngày tự động hủy đơn Self Drop-off nếu lab chưa nhận phim (BR-23). */
  SELF_DROPOFF_AUTO_CANCEL_DAYS(ParameterValueType.INTEGER),

  /** Thời hạn (giờ) để lab phản hồi chấp nhận hoặc từ chối đơn hàng mới. */
  LAB_RESPONSE_LIMIT_HOURS(ParameterValueType.INTEGER),

  /** Thời hạn (ngày) cho phép yêu cầu quét lại (re-scan) (BR-27). */
  RESCAN_WINDOW_DAYS(ParameterValueType.INTEGER),

  /** Hạn mức dung lượng lưu trữ số miễn phí (GB) cho mỗi Photographer. */
  FREE_STORAGE_QUOTA_GB(ParameterValueType.INTEGER),

  /** Hạn mức số câu hỏi tư vấn Chatbot AI cho mỗi user mỗi ngày. */
  CHATBOT_DAILY_QUESTION_QUOTA(ParameterValueType.INTEGER);

  private final ParameterValueType expectedType;

  SystemParameterKey(ParameterValueType expectedType) {
    this.expectedType = expectedType;
  }
}
