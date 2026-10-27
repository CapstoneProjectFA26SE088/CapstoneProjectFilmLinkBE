package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.CreateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.service.masterdata.MasterdataAdminService;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục loại dịch vụ ngành phim (MD-01, MD-02, UC53, BR-16, BR-32). */
@RestController
@RequestMapping("/api/v1/service-types")
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, MD-02, UC53, BR-16, BR-32)")
public class ServiceTypeController {

  private final MasterdataService masterdataService;
  private final MasterdataAdminService masterdataAdminService;

  public ServiceTypeController(
      MasterdataService masterdataService, MasterdataAdminService masterdataAdminService) {
    this.masterdataService = masterdataService;
    this.masterdataAdminService = masterdataAdminService;
  }

  @GetMapping
  @Operation(
      summary = "Lấy danh sách loại dịch vụ đang hoạt động (MD-01)",
      description =
          "API công khai (No-Auth), chỉ trả về các loại dịch vụ ACTIVE (DEVELOPING, SCANNING, PRINTING...).")
  public List<ServiceTypeResponse> getServiceTypes() {
    return masterdataService.getServiceTypes();
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Lấy toàn bộ danh sách loại dịch vụ (MD-02)",
      description = "Yêu cầu quyền ADMIN. Trả về cả các loại dịch vụ ACTIVE và DISCONTINUED.")
  public List<ServiceTypeResponse> getAllServiceTypes() {
    return masterdataAdminService.getAllServiceTypes();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "[Admin] Tạo mới loại dịch vụ (MD-02)",
      description = "Yêu cầu quyền ADMIN. Mã code viết UPPER_SNAKE_CASE và phải duy nhất toàn hệ thống (409 nếu trùng).")
  public ServiceTypeResponse createServiceType(@Valid @RequestBody CreateCatalogRequest request) {
    return masterdataAdminService.createServiceType(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Cập nhật loại dịch vụ (MD-02)",
      description = "Yêu cầu quyền ADMIN. Cho phép cập nhật tên hiển thị và mô tả. Mã code là bất biến (không cho sửa).")
  public ServiceTypeResponse updateServiceType(
      @PathVariable Long id, @Valid @RequestBody UpdateCatalogRequest request) {
    return masterdataAdminService.updateServiceType(id, request);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Chuyển đổi trạng thái loại dịch vụ (ACTIVE ↔ DISCONTINUED) (MD-02)",
      description = "Yêu cầu quyền ADMIN. Đổi trạng thái giữa ACTIVE và DISCONTINUED.")
  public ServiceTypeResponse toggleServiceTypeStatus(@PathVariable Long id) {
    return masterdataAdminService.toggleServiceTypeStatus(id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "[Admin] Xóa loại dịch vụ (MD-02)",
      description = "Yêu cầu quyền ADMIN. Chặn xóa cứng nếu đang có tham chiếu (422 Unprocessable Content); khi đó yêu cầu Admin chuyển sang DISCONTINUED.")
  public void deleteServiceType(@PathVariable Long id) {
    masterdataAdminService.deleteServiceType(id);
  }
}
