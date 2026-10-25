package com.fa26se088.filmlink.repository.lab;

import com.fa26se088.filmlink.entity.lab.FilmLabImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmLabImageRepository extends JpaRepository<FilmLabImage, Long> {

  List<FilmLabImage> findByFilmLabIdOrderByDisplayOrder(Long filmLabId);

  /** Ảnh {@code id} chỉ khi thuộc lab {@code filmLabId}; ảnh của lab khác coi như không tồn tại. */
  Optional<FilmLabImage> findByIdAndFilmLabId(Long id, Long filmLabId);
}
