package com.fa26se088.filmlink.dto.masterdata.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body tạo mới danh mục 3-field (FilmProcess, FilmFormat, ServiceType) (MD-02).
 *
 * <p>{@code code} là mã định danh bất biến, viết UPPER_SNAKE_CASE: chỉ gồm chữ IN HOA, số và
 * dấu gạch dưới, bắt đầu bằng chữ (ví dụ {@code PROCESS_C41}, {@code FORMAT_35MM}).
 */
public record CreateCatalogRequest(
    @NotBlank(message = "Mã danh mục không được để trống.")
        @Size(max = 20, message = "Mã danh mục tối đa 20 ký tự.")
        @Pattern(
            regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Mã danh mục chỉ gồm chữ IN HOA, số và dấu gạch dưới, bắt đầu bằng chữ.")
        String code,
    @NotBlank(message = "Tên hiển thị không được để trống.")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự.")
        String displayName,
    @Size(max = 500, message = "Mô tả tối đa 500 ký tự.") String description) {}
