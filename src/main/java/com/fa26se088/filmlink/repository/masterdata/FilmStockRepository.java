package com.fa26se088.filmlink.repository.masterdata;

import com.fa26se088.filmlink.entity.masterdata.FilmStock;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmStockRepository extends JpaRepository<FilmStock, Long> {

  @Query("""
      SELECT s FROM FilmStock s
      JOIN FETCH s.defaultFilmProcess
      WHERE s.status = :status
        AND (:query IS NULL OR :query = ''
             OR LOWER(s.stockName) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(s.manufacturer) LIKE LOWER(CONCAT('%', :query, '%')))
      ORDER BY s.manufacturer ASC, s.stockName ASC
      """)
  List<FilmStock> searchActiveFilmStocks(
      @Param("status") CatalogStatus status,
      @Param("query") String query);
}
