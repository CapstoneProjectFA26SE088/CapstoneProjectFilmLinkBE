package com.fa26se088.filmlink.repository.lab;

import com.fa26se088.filmlink.entity.lab.FilmLab;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmLabRepository extends JpaRepository<FilmLab, Long> {

  /** Mỗi Film Lab Owner có tối đa một lab ({@code uq_film_labs_owner}). */
  Optional<FilmLab> findByOwnerAccountId(Long ownerAccountId);
}
