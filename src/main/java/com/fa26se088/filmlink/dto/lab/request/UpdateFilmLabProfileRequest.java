package com.fa26se088.filmlink.dto.lab.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * UC12: Film Lab Owner sửa hồ sơ lab. Giới hạn khớp CHECK của {@code film_labs}.
 *
 * <p>Toạ độ nhập tay (chưa có Geocoding, UH-06): phải có cả hai hoặc trống cả hai — service kiểm.
 * {@code contactEmail}, {@code contactPhone} không sửa ở đây.
 *
 * @param expressDailyCapPercent BR-61: {@code null} = không giới hạn tỉ lệ đơn Express mỗi ngày
 */
public record UpdateFilmLabProfileRequest(
    @NotBlank @Size(min = 2, max = 150) String labName,
    @NotBlank @Size(min = 50, max = 2000) String description,
    @NotBlank @Size(max = 255) String addressLine,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 100) String province,
    @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 2, fraction = 6) BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6)
        BigDecimal longitude,
    @NotNull @Min(0) @Max(48) Short qcBufferHours,
    @Min(1) @Max(100) Short expressDailyCapPercent) {}
