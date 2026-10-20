package com.fa26se088.filmlink.repository.masterdata;

import com.fa26se088.filmlink.entity.masterdata.FilmProcess;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmProcessRepository extends JpaRepository<FilmProcess, Long> {

  List<FilmProcess> findByStatusOrderByDisplayNameAsc(CatalogStatus status);

  Optional<FilmProcess> findByCode(String code);
}
