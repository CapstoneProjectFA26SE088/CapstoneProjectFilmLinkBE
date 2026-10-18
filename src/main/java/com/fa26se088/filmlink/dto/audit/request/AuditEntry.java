package com.fa26se088.filmlink.dto.audit.request;

import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import java.util.Map;
import java.util.Objects;

/**
 * Lệnh ghi một dòng audit log, truyền vào {@code AuditLogService} (không phải body HTTP).
 *
 * <p>Không có actor: {@code AuditLogService} tự lấy tài khoản đang đăng nhập ({@code
 * CurrentAccount.find()}); không có thì ghi là System.
 *
 * <p>{@code before} / {@code after} chỉ chứa giá trị đơn giản: String, số, boolean, enum dạng
 * String ({@code name()}), Instant dạng ISO-8601 ({@code toString()}). <b>Không</b> đưa entity vào,
 * <b>không</b> đưa dữ liệu nhạy cảm (password hash, OTP, refresh token, secret): audit log lưu tối
 * thiểu 12 tháng và không xoá được.
 *
 * @param action thao tác, bắt buộc
 * @param targetType loại bản ghi bị tác động, bắt buộc
 * @param targetId id bản ghi bị tác động, có thể {@code null}
 * @param reason lý do (tối đa 500 ký tự), có thể {@code null}
 * @param before dữ liệu trước thay đổi (lưu jsonb), có thể {@code null}; xem quy tắc ở trên
 * @param after dữ liệu sau thay đổi (lưu jsonb), có thể {@code null}; xem quy tắc ở trên
 */
public record AuditEntry(
    AuditAction action,
    AuditTargetType targetType,
    Long targetId,
    String reason,
    Map<String, Object> before,
    Map<String, Object> after) {

  public AuditEntry {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(targetType, "targetType");
  }
}
