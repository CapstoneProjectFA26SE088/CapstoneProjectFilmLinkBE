package com.fa26se088.filmlink.mapper.audit;

import com.fa26se088.filmlink.dto.audit.response.AuditLogResponse;
import com.fa26se088.filmlink.entity.audit.AuditLog;
import org.springframework.stereotype.Component;

/** Chuyển {@link AuditLog} sang DTO. Viết tay, không dùng MapStruct. */
@Component
public class AuditLogMapper {

  public AuditLogResponse toResponse(AuditLog log) {
    return new AuditLogResponse(
        log.getId(),
        log.getOccurredAt(),
        log.getActorAccountId(),
        log.getActorRole(),
        log.getAction(),
        log.getTargetType(),
        log.getTargetId(),
        log.getOutcome(),
        log.getReason(),
        log.getBeforeData(),
        log.getAfterData(),
        log.getClientIp() == null ? null : log.getClientIp().getHostAddress());
  }
}
