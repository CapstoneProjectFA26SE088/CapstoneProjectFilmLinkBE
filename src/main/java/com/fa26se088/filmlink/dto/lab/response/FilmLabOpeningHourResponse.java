package com.fa26se088.filmlink.dto.lab.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

/** Giờ mở cửa của một ngày; {@code dayOfWeek} theo ISO-8601 (1 = Thứ Hai … 7 = Chủ nhật). */
public record FilmLabOpeningHourResponse(
    Short dayOfWeek,
    boolean closed,
    @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "08:00") LocalTime opensAt,
    @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:00")
        LocalTime closesAt) {}
