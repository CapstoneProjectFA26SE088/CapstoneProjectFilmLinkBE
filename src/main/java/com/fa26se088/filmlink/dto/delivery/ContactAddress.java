package com.fa26se088.filmlink.dto.delivery;

public record ContactAddress(
        String name,
        String phone,
        String street,        // số nhà, tên đường
        String wardCode,
        String districtCode,
        String provinceCode
) {}
