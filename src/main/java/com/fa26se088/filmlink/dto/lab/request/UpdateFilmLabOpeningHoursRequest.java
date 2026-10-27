package com.fa26se088.filmlink.dto.lab.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** UC12: thay toàn bộ giờ mở cửa, đúng 7 ngày, mỗi ngày một lần (service kiểm trùng). */
public record UpdateFilmLabOpeningHoursRequest(
    @NotNull @Size(min = 7, max = 7) List<@NotNull @Valid FilmLabOpeningHourRequest> openingHours) {}
