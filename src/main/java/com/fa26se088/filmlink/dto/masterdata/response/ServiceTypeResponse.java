package com.fa26se088.filmlink.dto.masterdata.response;

import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;

/** Thông tin loại dịch vụ trả cho client (BR-16). */
public record ServiceTypeResponse(
    Long id,
    String code,
    String displayName,
    String description,
    CatalogStatus status) {}
