package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.CreateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.service.masterdata.MasterdataAdminService;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục dòng phim thương mại (MD-01, MD-02, UC53, BR-16, BR-32). */
@RestController
@RequestMapping("/api/v1/film-stocks")
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, MD-02, UC53, BR-16, BR-32)")
public class FilmStockController {

  private final MasterdataService masterdataService;
  private final MasterdataAdminService masterdataAdminService;

  public FilmStockController(
      MasterdataService masterdataService, MasterdataAdminService masterdataAdminService) {
    this.masterdataService = masterdataService;
    this.masterdataAdminService = masterdataAdminService;
  }

  @GetMapping
  @Operation(
      summary = "Lấy danh sách dòng phim thương mại đang hoạt động (MD-01)",
      description =
          "API công khai (No-Auth), chỉ trả về các dòng phim ACTIVE. Hỗ trợ tìm kiếm theo tên dòng phim hoặc tên hãng.")
  public List<FilmStockResponse> getFilmStocks(
      @Parameter(description = "Từ khoá tìm kiếm theo tên phim hoặc hãng (ví dụ: Gold, Kodak, Cinestill)")
          @RequestParam(name = "q", required = false)
          String query) {
    return masterdataService.getFilmStocks(query);
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Lấy toàn bộ danh sách dòng phim thương mại (MD-02)",
      description = "Yêu cầu quyền ADMIN. Trả về cả các dòng phim ACTIVE và DISCONTINUED.")
  public List<FilmStockResponse> getAllFilmStocks(
      @Parameter(description = "Từ khoá tìm kiếm theo tên phim hoặc hãng")
          @RequestParam(name = "q", required = false)
          String query) {
    return masterdataAdminService.getAllFilmStocks(query);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "[Admin] Tạo mới dòng phim (MD-02)",
      description = "Yêu cầu quyền ADMIN. Cặp (stockName, manufacturer) phải duy nhất (409 nếu trùng).")
  public FilmStockResponse createFilmStock(@Valid @RequestBody CreateFilmStockRequest request) {
    return masterdataAdminService.createFilmStock(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Cập nhật dòng phim (MD-02)",
      description = "Yêu cầu quyền ADMIN. Cho phép cập nhật thông tin hiển thị, ISO, hãng và quy trình mặc định.")
  public FilmStockResponse updateFilmStock(
      @PathVariable Long id, @Valid @RequestBody UpdateFilmStockRequest request) {
    return masterdataAdminService.updateFilmStock(id, request);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "[Admin] Chuyển đổi trạng thái dòng phim (ACTIVE ↔ DISCONTINUED) (MD-02)",
      description = "Yêu cầu quyền ADMIN. Đổi trạng thái giữa ACTIVE và DISCONTINUED.")
  public FilmStockResponse toggleFilmStockStatus(@PathVariable Long id) {
    return masterdataAdminService.toggleFilmStockStatus(id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "[Admin] Xóa dòng phim (MD-02)",
      description = "Yêu cầu quyền ADMIN. Chặn xóa cứng nếu đang có tham chiếu (422 Unprocessable Content); khi đó yêu cầu Admin chuyển sang DISCONTINUED.")
  public void deleteFilmStock(@PathVariable Long id) {
    masterdataAdminService.deleteFilmStock(id);
  }
}
