package com.fa26se088.filmlink.dto.lab.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Giờ mở cửa của một ngày (UC12). Ngày nghỉ: {@code closed = true}, không gửi giờ. Ngày mở: có cả
 * {@code opensAt} và {@code closesAt}, {@code opensAt < closesAt} — service kiểm.
 *
 * @param dayOfWeek ISO-8601: 1 = Thứ Hai … 7 = Chủ nhật
 */
public record FilmLabOpeningHourRequest(
    @NotNull @Min(1) @Max(7) Short dayOfWeek,
    @NotNull Boolean closed,
    @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "08:00") LocalTime opensAt,
    @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:00")
        LocalTime closesAt) {}
