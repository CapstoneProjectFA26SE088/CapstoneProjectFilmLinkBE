package com.fa26se088.filmlink.dto.masterdata.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cập nhật danh mục 3-field (FilmProcess, FilmFormat, ServiceType) (MD-02).
 *
 * <p>Không có trường {@code code}: mã định danh là bất biến (Immutable) sau khi khởi tạo.
 */
public record UpdateCatalogRequest(
    @NotBlank(message = "Tên hiển thị không được để trống.")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự.")
        String displayName,
    @Size(max = 500, message = "Mô tả tối đa 500 ký tự.") String description) {}
