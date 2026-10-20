package com.fa26se088.filmlink.dto.masterdata.response;

import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;

/** Thông tin dòng phim thương mại trả cho client (BR-16). */
public record FilmStockResponse(
    Long id,
    String stockName,
    String manufacturer,
    Short iso,
    Long defaultFilmProcessId,
    String defaultFilmProcessName,
    CatalogStatus status) {}
