package com.fa26se088.filmlink.dto.masterdata.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body cập nhật dòng phim (FilmStock) (MD-02).
 *
 * <p>
 * Cho phép chỉnh sửa tên hiển thị, hãng, ISO và quy trình hóa chất mặc định.
 */
public record UpdateFilmStockRequest(
        @NotBlank(message = "Tên dòng phim không được để trống.") @Size(max = 100, message = "Tên dòng phim tối đa 100 ký tự.") String stockName,
        @NotBlank(message = "Tên hãng sản xuất không được để trống.") @Size(max = 100, message = "Tên hãng sản xuất tối đa 100 ký tự.") String manufacturer,
        @NotNull(message = "ISO không được để trống.") @Min(value = 1, message = "ISO phải lớn hơn 0.") @Max(value = 32000, message = "ISO tối đa 32000.") Short iso,
        @NotNull(message = "Quy trình hóa chất mặc định không được để trống.") Long defaultFilmProcessId) {
}
