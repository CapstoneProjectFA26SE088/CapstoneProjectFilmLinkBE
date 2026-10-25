package com.fa26se088.filmlink.repository.lab;

import com.fa26se088.filmlink.entity.lab.FilmLab;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FilmLabRepository extends JpaRepository<FilmLab, Long> {

  /** Mỗi Film Lab Owner có tối đa một lab ({@code uq_film_labs_owner}). */
  Optional<FilmLab> findByOwnerAccountId(Long ownerAccountId);

  /** Một đơn đăng ký được duyệt tạo tối đa một lab ({@code uq_film_labs_application}). */
  boolean existsByLabRegistrationApplicationId(Long labRegistrationApplicationId);

  /**
   * {@code SELECT ... FOR UPDATE} trên dòng lab: tuần tự hoá các thao tác sửa ảnh / giờ mở cửa của
   * cùng một lab (đếm giới hạn 20 ảnh, chọn {@code display_order} trống, upsert theo ngày).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from FilmLab l where l.id = :id")
  Optional<FilmLab> findByIdForUpdate(@Param("id") Long id);
}
