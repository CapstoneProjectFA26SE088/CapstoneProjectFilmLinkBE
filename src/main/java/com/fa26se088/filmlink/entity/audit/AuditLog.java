package com.fa26se088.filmlink.entity.audit;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditOutcome;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Audit Log (ERD #53), bảng {@code audit_logs} (BR-32, CR-16, NFR-SEC-08).
 *
 * <p>Append-only: bảng không có {@code created_at}/{@code updated_at} nên <b>không</b> kế thừa
 * {@code BaseEntity}. {@link Immutable}: Hibernate bỏ qua mọi thay đổi sau khi insert; DB còn có
 * trigger {@code audit_logs_read_only} chặn UPDATE và DELETE dòng chưa đủ 12 tháng.
 *
 * <p>Chỉ tạo qua {@code AuditLogService}. {@link #actorAccountId} là id thuần (không
 * {@code @ManyToOne}) để không phải nạp {@code UserAccount} khi ghi hay đọc log.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  /** {@code null} khi actor là System (Scheduler). */
  @Column(name = "actor_account_id")
  private Long actorAccountId;

  /** Role tại thời điểm thao tác (role có thể đổi sau, ví dụ nâng hạng Expert). */
  @Enumerated(EnumType.STRING)
  @Column(name = "actor_role", length = 30)
  private UserAccountRole actorRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 100)
  private AuditAction action;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 50)
  private AuditTargetType targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome", nullable = false, length = 20)
  private AuditOutcome outcome;

  @Column(name = "reason", length = 500)
  private String reason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before_data")
  private Map<String, Object> beforeData;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after_data")
  private Map<String, Object> afterData;

  @JdbcTypeCode(SqlTypes.INET)
  @Column(name = "client_ip")
  private InetAddress clientIp;
}
