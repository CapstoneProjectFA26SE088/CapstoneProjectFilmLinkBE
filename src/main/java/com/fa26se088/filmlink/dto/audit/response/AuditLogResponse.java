package com.fa26se088.filmlink.dto.audit.response;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditOutcome;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import java.time.Instant;
import java.util.Map;

/**
 * Một dòng audit log trả cho Admin. {@code actorAccountId} và {@code actorRole} cùng {@code null}
 * khi actor là System.
 */
public record AuditLogResponse(
    Long id,
    Instant occurredAt,
    Long actorAccountId,
    UserAccountRole actorRole,
    AuditAction action,
    AuditTargetType targetType,
    Long targetId,
    AuditOutcome outcome,
    String reason,
    Map<String, Object> beforeData,
    Map<String, Object> afterData,
    String clientIp) {}
