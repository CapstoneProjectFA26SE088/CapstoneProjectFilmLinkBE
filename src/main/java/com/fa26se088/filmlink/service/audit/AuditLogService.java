package com.fa26se088.filmlink.service.audit;

import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.audit.response.AuditLogResponse;
import com.fa26se088.filmlink.dto.common.PageResponse;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import org.springframework.data.domain.Pageable;

/**
 * BR-32 (CR-16, NFR-SEC-08): ghi audit log cho thao tác nhạy cảm — actor, action, target, thời
 * điểm, kết quả. Log chỉ ghi thêm, không sửa; giữ tối thiểu 12 tháng.
 *
 * <p>Cách dùng từ service domain khác (gọi trong method {@code @Transactional} của nghiệp vụ):
 *
 * <pre>{@code
 * auditLogService.recordSuccess(new AuditEntry(
 *     AuditAction.ACCOUNT_PASSWORD_CHANGED, AuditTargetType.USER_ACCOUNT, accountId,
 *     null, null, null));
 * }</pre>
 *
 * <p>Actor do service tự lấy bằng {@code CurrentAccount.find()}: có tài khoản đăng nhập thì ghi id
 * và role hiện tại; không có (Scheduler, thread không có {@code SecurityContext}) thì ghi là
 * System. Listener {@code @Async} không mang {@code SecurityContext} nên sẽ bị ghi là System —
 * <b>không</b> gọi audit từ listener async.
 *
 * <p>Không ghi audit cho login / refresh / register / verify-email (BR-32 không yêu cầu).
 */
public interface AuditLogService {

  /**
   * BR-32: ghi thao tác <b>thành công</b>. Chạy chung transaction của nghiệp vụ: nghiệp vụ rollback
   * thì dòng audit cũng mất (việc chưa xảy ra thì không ghi).
   */
  void recordSuccess(AuditEntry entry);

  /**
   * BR-32: ghi thao tác <b>thất bại</b> (ví dụ bị từ chối vì sai quyền). Chạy transaction riêng
   * nên vẫn được lưu khi transaction nghiệp vụ rollback.
   *
   * <p><b>Cảnh báo treo:</b> không gọi khi transaction nghiệp vụ đang giữ khoá {@code
   * PESSIMISTIC_WRITE} ({@code SELECT ... FOR UPDATE}) trên dòng {@code user_accounts} của chính
   * actor. Transaction REQUIRES_NEW insert audit cần khoá FK ({@code KEY SHARE}) trên dòng đó nên
   * phải chờ transaction ngoài, còn transaction ngoài lại chờ {@code recordFailure} trả về → treo.
   * Trường hợp đó gọi {@code recordFailure} sau khi transaction nghiệp vụ đã kết thúc.
   */
  void recordFailure(AuditEntry entry);

  /**
   * Admin xem audit log, mới nhất trước. {@code targetType}, {@code targetId} tuỳ chọn; sort luôn
   * là {@code occurredAt} DESC, bỏ qua sort client gửi lên.
   */
  PageResponse<AuditLogResponse> search(
      AuditTargetType targetType, Long targetId, Pageable pageable);
}
