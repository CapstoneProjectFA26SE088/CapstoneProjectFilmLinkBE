package com.fa26se088.filmlink.entity.masterdata;

import com.fa26se088.filmlink.entity.BaseEntity;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Dòng phim thương mại (Portra 400, Gold 200, HP5 Plus...) · BR-16, UC53. */
@Getter
@Setter
@Entity
@Table(name = "film_stocks")
public class FilmStock extends BaseEntity {

  @Column(name = "stock_name", nullable = false, length = 100)
  private String stockName;

  @Column(name = "manufacturer", nullable = false, length = 100)
  private String manufacturer;

  @Column(name = "iso", nullable = false)
  private Short iso;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "default_film_process_id", nullable = false)
  private FilmProcess defaultFilmProcess;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CatalogStatus status = CatalogStatus.ACTIVE;
}
