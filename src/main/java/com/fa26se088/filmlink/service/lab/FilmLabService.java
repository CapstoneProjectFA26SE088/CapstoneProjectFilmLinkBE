package com.fa26se088.filmlink.service.lab;

import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabAcceptingOrdersRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabOpeningHoursRequest;
import com.fa26se088.filmlink.dto.lab.request.UpdateFilmLabProfileRequest;
import com.fa26se088.filmlink.dto.lab.response.FilmLabImageResponse;
import com.fa26se088.filmlink.dto.lab.response.FilmLabProfileResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC12: Film Lab Owner quản lý hồ sơ lab của chính mình. Mọi method nhận {@code ownerAccountId}
 * của tài khoản đang đăng nhập; lab được tra theo chủ ({@link LabAccessService}), không nhận
 * {@code labId} từ client.
 *
 * <p>Lab được tạo khi đơn đăng ký được duyệt: {@code FilmLabServiceImpl} lắng nghe {@code
 * LabRegistrationApprovedEvent} (BR-03).
 */
public interface FilmLabService {

  /** Hồ sơ đầy đủ: thông tin lab, giờ mở cửa, ảnh mẫu (kèm link ký có hạn), trạng thái nhận đơn. */
  FilmLabProfileResponse getMyProfile(Long ownerAccountId);

  /** Sửa thông tin hồ sơ (không gồm email / số điện thoại liên hệ). */
  FilmLabProfileResponse updateMyProfile(Long ownerAccountId, UpdateFilmLabProfileRequest request);

  /** Thay toàn bộ giờ mở cửa 7 ngày. */
  FilmLabProfileResponse updateMyOpeningHours(
      Long ownerAccountId, UpdateFilmLabOpeningHoursRequest request);

  /** Thêm một ảnh mẫu (tối đa 20 ảnh mỗi lab). */
  FilmLabImageResponse uploadMyImage(Long ownerAccountId, MultipartFile file);

  /** Xoá một ảnh mẫu của lab mình. */
  void deleteMyImage(Long ownerAccountId, Long imageId);

  /** Bật/tắt nhận đơn (BR-57). */
  FilmLabProfileResponse updateMyAcceptingOrders(
      Long ownerAccountId, UpdateFilmLabAcceptingOrdersRequest request);
}
