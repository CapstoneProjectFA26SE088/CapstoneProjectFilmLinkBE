package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.CreateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
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

/** Danh mục quy trình hóa chất ngành phim (MD-01, MD-02, UC53, BR-16, BR-32). */
@RestController
@RequestMapping("/api/v1/film-processes")
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, MD-02, UC53, BR-16, BR-32)")
public class FilmProcessController {

  private final MasterdataService masterdataService;
  private final MasterdataAdminService masterdataAdminService;

  public FilmProcessController(
      MasterdataService masterdataService, MasterdataAdminService masterdataAdminService) {
    this.masterdataService = masterdataService;
    this.masterdataAdminService = masterdataAdminService;
  }

  @GetMapping
  @Operation(
      summary = "Lấy danh sách quy trình hóa chất đang hoạt động (MD-01)",
      description = "API công khai (No-Auth), chỉ trả về các quy trình ACTIVE (C-41, B&W, E-6...).")
  public List<FilmProcessResponse> getFilmProcesses() {
    return masterdataService.getFilmProcesses();
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Lấy toàn bộ danh sách quy trình hóa chất (MD-02)",
      description = "Yêu cầu quyền ADMIN. Trả về cả các quy trình ACTIVE và DISCONTINUED.")
  public List<FilmProcessResponse> getAllFilmProcesses() {
    return masterdataAdminService.getAllFilmProcesses();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "[Admin] Tạo mới quy trình hóa chất (MD-02)",
      description = "Yêu cầu quyền ADMIN. Mã code viết UPPER_SNAKE_CASE và phải duy nhất toàn hệ thống (409 nếu trùng).")
  public FilmProcessResponse createFilmProcess(@Valid @RequestBody CreateCatalogRequest request) {
    return masterdataAdminService.createFilmProcess(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Cập nhật quy trình hóa chất (MD-02)",
      description = "Yêu cầu quyền ADMIN. Cho phép cập nhật tên hiển thị và mô tả. Mã code là bất biến (không cho sửa).")
  public FilmProcessResponse updateFilmProcess(
      @PathVariable Long id, @Valid @RequestBody UpdateCatalogRequest request) {
    return masterdataAdminService.updateFilmProcess(id, request);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Chuyển đổi trạng thái quy trình (ACTIVE ↔ DISCONTINUED) (MD-02)",
      description = "Yêu cầu quyền ADMIN. Đổi trạng thái giữa ACTIVE và DISCONTINUED.")
  public FilmProcessResponse toggleFilmProcessStatus(@PathVariable Long id) {
    return masterdataAdminService.toggleFilmProcessStatus(id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "[Admin] Xóa quy trình hóa chất (MD-02)",
      description = "Yêu cầu quyền ADMIN. Chặn xóa cứng nếu quy trình đang được tham chiếu bởi film_stocks (422 Unprocessable Content); khi đó yêu cầu Admin chuyển sang DISCONTINUED.")
  public void deleteFilmProcess(@PathVariable Long id) {
    masterdataAdminService.deleteFilmProcess(id);
  }
}
