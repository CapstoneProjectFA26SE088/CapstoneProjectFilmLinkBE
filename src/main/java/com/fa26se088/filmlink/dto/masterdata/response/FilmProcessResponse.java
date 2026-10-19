package com.fa26se088.filmlink.dto.masterdata.response;

import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;

/** Thông tin quy trình tráng phim hóa chất trả cho client (BR-16). */
public record FilmProcessResponse(
    Long id,
    String code,
    String displayName,
    String description,
    CatalogStatus status) {}
