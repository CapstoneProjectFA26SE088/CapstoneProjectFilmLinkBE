package com.fa26se088.filmlink.dto.lab.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Hồ sơ lab cho chính Film Lab Owner (UC12): thông tin lab, giờ mở cửa (theo ngày), ảnh mẫu (theo
 * {@code displayOrder}) và trạng thái nhận đơn.
 */
public record FilmLabProfileResponse(
    Long id,
    String labName,
    String description,
    String addressLine,
    String district,
    String province,
    BigDecimal latitude,
    BigDecimal longitude,
    String contactEmail,
    String contactPhone,
    boolean acceptingOrders,
    Short qcBufferHours,
    Short expressDailyCapPercent,
    List<FilmLabOpeningHourResponse> openingHours,
    List<FilmLabImageResponse> images,
    Instant createdAt,
    Instant updatedAt) {}
