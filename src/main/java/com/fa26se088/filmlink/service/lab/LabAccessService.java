package com.fa26se088.filmlink.service.lab;

import com.fa26se088.filmlink.entity.lab.FilmLab;

/**
 * Kiểm Film Lab Owner chỉ thao tác trên lab của chính mình (CR-14: phân quyền ở tầng service).
 *
 * <p>Service của domain khác (machine, order, ...) gọi interface này thay vì tự viết lại phép kiểm
 * hoặc inject {@code FilmLabRepository} (R4). Trả entity vì chỉ dùng giữa các service, trong cùng
 * transaction của service gọi; không trả entity này ra controller (R2).
 */
public interface LabAccessService {

  /**
   * Lab của Film Lab Owner. Mỗi chủ có đúng một lab ({@code uq_film_labs_owner}).
   *
   * @throws com.fa26se088.filmlink.exception.BusinessException {@code LAB_NOT_FOUND} (404) khi
   *     tài khoản chưa có lab
   */
  FilmLab requireOwnedLab(Long ownerAccountId);

  /**
   * Lab {@code labId}, chỉ khi nó thuộc {@code ownerAccountId}.
   *
   * @throws com.fa26se088.filmlink.exception.BusinessException {@code LAB_NOT_FOUND} (404) khi lab
   *     không tồn tại; {@code LAB_ACCESS_DENIED} (403) khi lab thuộc chủ khác
   */
  FilmLab requireOwnedLab(Long ownerAccountId, Long labId);
}
