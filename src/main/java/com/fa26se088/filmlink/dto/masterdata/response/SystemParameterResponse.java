package com.fa26se088.filmlink.dto.masterdata.response;

import com.fa26se088.filmlink.entity.masterdata.enums.ParameterValueType;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import java.time.Instant;

/** Response trả về thông tin chi tiết của một tham số hệ thống. */
public record SystemParameterResponse(
    Long id,
    SystemParameterKey key,
    String value,
    ParameterValueType valueType,
    String description,
    Integer version,
    Long updatedByAccountId,
    Instant updatedAt) {}
