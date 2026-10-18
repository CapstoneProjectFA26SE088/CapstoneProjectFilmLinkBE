package com.fa26se088.filmlink.repository.audit;

import com.fa26se088.filmlink.entity.audit.AuditLog;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Chỉ insert và đọc; không có method sửa/xoá vì bảng append-only (BR-32). */
public interface AuditLogRepository
    extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

  /**
   * Điều kiện lọc cho màn hình Admin. Tham số {@code null} thì bỏ qua điều kiện đó. Lọc cả hai dùng
   * index {@code ix_audit_logs_target}.
   */
  static Specification<AuditLog> byTarget(AuditTargetType targetType, Long targetId) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>(2);
      if (targetType != null) {
        predicates.add(cb.equal(root.get("targetType"), targetType));
      }
      if (targetId != null) {
        predicates.add(cb.equal(root.get("targetId"), targetId));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
