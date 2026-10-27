package com.fa26se088.filmlink.controller.lab;

import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabAcceptingOrdersRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabOpeningHoursRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabProfileRequest;
import com.fa26se088.filmlink.dto.lab.response.FilmLabImageResponse;
import com.fa26se088.filmlink.dto.lab.response.FilmLabProfileResponse;
import com.fa26se088.filmlink.security.AuthenticatedAccount;
import com.fa26se088.filmlink.service.lab.FilmLabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC12: Film Lab Owner quản lý hồ sơ lab của chính mình. Lab luôn là lab của tài khoản đang đăng
 * nhập ({@code /me}), không nhận {@code labId} từ client.
 */
@RestController
@RequestMapping("/api/v1/film-labs/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FILM_LAB_OWNER')")
@Tag(name = "Film Lab Profile", description = "Film Lab Owner quản lý hồ sơ lab (UC12)")
public class FilmLabController {

  private final FilmLabService filmLabService;

  @GetMapping
  @Operation(summary = "Hồ sơ lab: thông tin, giờ mở cửa, ảnh mẫu, trạng thái nhận đơn")
  public FilmLabProfileResponse getMyProfile(
      @AuthenticationPrincipal AuthenticatedAccount account) {
    return filmLabService.getMyProfile(account.id());
  }

  @PutMapping
  @Operation(summary = "Sửa thông tin hồ sơ lab (toạ độ nhập tay, cả hai hoặc không)")
  public FilmLabProfileResponse updateMyProfile(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody UpdateFilmLabProfileRequest request) {
    return filmLabService.updateMyProfile(account.id(), request);
  }

  @PutMapping("/opening-hours")
  @Operation(summary = "Thay toàn bộ giờ mở cửa 7 ngày (ISO: 1 = Thứ Hai … 7 = Chủ nhật)")
  public FilmLabProfileResponse updateMyOpeningHours(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody UpdateFilmLabOpeningHoursRequest request) {
    return filmLabService.updateMyOpeningHours(account.id(), request);
  }

  @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Upload một ảnh mẫu (JPG/PNG, tối đa 10 MB, tối đa 20 ảnh mỗi lab)")
  public FilmLabImageResponse uploadMyImage(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestPart("file") MultipartFile file) {
    return filmLabService.uploadMyImage(account.id(), file);
  }

  @DeleteMapping("/images/{imageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Xoá một ảnh mẫu")
  public void deleteMyImage(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable Long imageId) {
    filmLabService.deleteMyImage(account.id(), imageId);
  }

  @PatchMapping("/accepting-orders")
  @Operation(summary = "Bật/tắt nhận đơn; bật cần ít nhất 1 PROCESSOR và 1 SCANNER ACTIVE (BR-57)")
  public FilmLabProfileResponse updateMyAcceptingOrders(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody UpdateFilmLabAcceptingOrdersRequest request) {
    return filmLabService.updateMyAcceptingOrders(account.id(), request);
  }
}
