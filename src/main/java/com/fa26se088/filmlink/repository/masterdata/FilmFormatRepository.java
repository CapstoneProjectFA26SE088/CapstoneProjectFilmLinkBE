package com.fa26se088.filmlink.repository.masterdata;

import com.fa26se088.filmlink.entity.masterdata.FilmFormat;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmFormatRepository extends JpaRepository<FilmFormat, Long> {

  List<FilmFormat> findByStatusOrderByDisplayNameAsc(CatalogStatus status);

  Optional<FilmFormat> findByCode(String code);
}
