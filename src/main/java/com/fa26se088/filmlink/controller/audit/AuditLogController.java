package com.fa26se088.filmlink.controller.audit;

import com.fa26se088.filmlink.dto.audit.response.AuditLogResponse;
import com.fa26se088.filmlink.dto.common.PageResponse;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin xem audit log (BR-32, CR-16). */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Admin xem lịch sử thao tác nhạy cảm")
public class AuditLogController {

  private final AuditLogService auditLogService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Danh sách audit log, mới nhất trước; lọc tuỳ chọn theo targetType, targetId")
  public PageResponse<AuditLogResponse> search(
      @RequestParam(required = false) AuditTargetType targetType,
      @RequestParam(required = false) Long targetId,
      @PageableDefault(size = 20) Pageable pageable) {
    return auditLogService.search(targetType, targetId, pageable);
  }
}
