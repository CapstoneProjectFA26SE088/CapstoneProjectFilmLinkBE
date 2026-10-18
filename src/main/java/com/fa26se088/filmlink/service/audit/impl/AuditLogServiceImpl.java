package com.fa26se088.filmlink.service.audit.impl;

import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.audit.response.AuditLogResponse;
import com.fa26se088.filmlink.dto.common.PageResponse;
import com.fa26se088.filmlink.entity.audit.AuditLog;
import com.fa26se088.filmlink.entity.audit.enums.AuditOutcome;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.mapper.audit.AuditLogMapper;
import com.fa26se088.filmlink.repository.audit.AuditLogRepository;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.security.CurrentAccount;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

  /** Mới nhất trước (khớp {@code ix_audit_logs_target}); {@code id} giữ thứ tự ổn định khi trùng giờ. */
  private static final Sort NEWEST_FIRST =
      Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

  private final AuditLogRepository auditLogRepository;
  private final AuditLogMapper auditLogMapper;

  /** BR-32: propagation REQUIRED — rollback cùng nghiệp vụ. */
  @Override
  @Transactional
  public void recordSuccess(AuditEntry entry) {
    auditLogRepository.save(toEntity(entry, AuditOutcome.SUCCESS));
  }

  /** BR-32: REQUIRES_NEW — vẫn lưu khi transaction nghiệp vụ rollback. */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(AuditEntry entry) {
    auditLogRepository.save(toEntity(entry, AuditOutcome.FAILURE));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> search(
      AuditTargetType targetType, Long targetId, Pageable pageable) {
    Pageable sorted =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    return PageResponse.from(
        auditLogRepository.findAll(AuditLogRepository.byTarget(targetType, targetId), sorted),
        auditLogMapper::toResponse);
  }

  /**
   * Actor và role lấy từ tài khoản đang đăng nhập ({@link CurrentAccount#find()}), tức role tại
   * thời điểm thao tác; không có tài khoản (System) thì cả hai null.
   */
  private static AuditLog toEntity(AuditEntry entry, AuditOutcome outcome) {
    AuthenticatedAccount actor = CurrentAccount.find().orElse(null);
    AuditLog log = new AuditLog();
    log.setOccurredAt(Instant.now());
    log.setActorAccountId(actor == null ? null : actor.id());
    log.setActorRole(actor == null ? null : actor.role());
    log.setAction(entry.action());
    log.setTargetType(entry.targetType());
    log.setTargetId(entry.targetId());
    log.setOutcome(outcome);
    log.setReason(entry.reason());
    log.setBeforeData(entry.before());
    log.setAfterData(entry.after());
    log.setClientIp(currentClientIp());
    return log;
  }

  /**
   * IP của client lấy từ {@code request.getRemoteAddr()} khi đang trong HTTP request; ngoài HTTP
   * request (Scheduler, listener chạy async) trả {@code null}.
   *
   * <p>Cố ý <b>không</b> đọc {@code X-Forwarded-For}: header này do client tự gửi được, đọc vào sẽ
   * cho phép giả mạo IP trong audit log. Khi deploy sau reverse proxy, cấu hình proxy tin cậy ở tầng
   * server (ví dụ {@code server.forward-headers-strategy}) để {@code getRemoteAddr()} trả đúng IP.
   */
  static InetAddress currentClientIp() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
      return null;
    }
    return parseIpLiteral(attrs.getRequest().getRemoteAddr());
  }

  /**
   * Chỉ nhận IP dạng số (IPv4/IPv6): ký tự hex, '.' hoặc ':', và phải có ít nhất một '.' hoặc ':'.
   * Chuỗi toàn chữ hex như {@code "abc"} là tên máy, sẽ làm {@link InetAddress#getByName} tra DNS.
   */
  static InetAddress parseIpLiteral(String address) {
    if (address == null
        || (address.indexOf('.') < 0 && address.indexOf(':') < 0)
        || !address.chars().allMatch(c -> Character.digit(c, 16) >= 0 || c == '.' || c == ':')) {
      return null;
    }
    try {
      return InetAddress.getByName(address);
    } catch (UnknownHostException ex) {
      return null;
    }
  }
}
