package com.fa26se088.filmlink.dto.lab.request;

import jakarta.validation.constraints.NotNull;

/** UC12: bật/tắt nhận đơn. Bật cần đủ máy theo BR-57. */
public record UpdateFilmLabAcceptingOrdersRequest(@NotNull Boolean acceptingOrders) {}
