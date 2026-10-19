package com.fa26se088.filmlink.entity.masterdata;

import com.fa26se088.filmlink.entity.BaseEntity;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Quy trình tráng phim hóa chất (C-41, B&W, E-6, ECN-2...) · BR-16, UC53. */
@Getter
@Setter
@Entity
@Table(name = "film_processes")
public class FilmProcess extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 20)
  private String code;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "description", length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CatalogStatus status = CatalogStatus.ACTIVE;
}
