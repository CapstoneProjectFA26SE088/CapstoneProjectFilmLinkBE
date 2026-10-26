package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.UpdateSystemParameterRequest;
import com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.masterdata.SystemParameterAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị tham số hệ thống dành cho Admin (UC54, BR-38). */
@RestController
@RequestMapping("/api/v1/admin/system-parameters")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
    name = "Master Data - System Parameters",
    description = "Quản trị tham số hệ thống toàn sàn (UC54, BR-38)")
public class SystemParameterAdminController {

  private final SystemParameterAdminService systemParameterAdminService;

  @GetMapping
  @Operation(
      summary = "[Admin] Lấy danh sách toàn bộ tham số hệ thống (UC54)",
      description = "Yêu cầu quyền ADMIN. Trả về danh sách tham số kèm version để hỗ trợ optimistic locking.")
  public List<SystemParameterResponse> getAllParameters() {
    return systemParameterAdminService.getAllParameters();
  }

  @GetMapping("/{key}")
  @Operation(
      summary = "[Admin] Lấy chi tiết tham số hệ thống theo key (UC54)",
      description = "Yêu cầu quyền ADMIN. Trả về thông tin tham số theo enum key.")
  public SystemParameterResponse getParameter(@PathVariable SystemParameterKey key) {
    return systemParameterAdminService.getParameter(key);
  }

  @PutMapping("/{key}")
  @Operation(
      summary = "[Admin] Cập nhật tham số hệ thống (UC54, BR-38)",
      description =
          "Yêu cầu quyền ADMIN. Có kiểm tra miền giá trị, ràng buộc thứ tự chế tài, optimistic lock và ghi audit log.")
  public SystemParameterResponse updateParameter(
      @PathVariable SystemParameterKey key,
      @Valid @RequestBody UpdateSystemParameterRequest request,
      @AuthenticationPrincipal AuthenticatedAccount account) {
    Long adminAccountId = account != null ? account.id() : null;
    return systemParameterAdminService.updateParameter(key, request, adminAccountId);
  }
}
