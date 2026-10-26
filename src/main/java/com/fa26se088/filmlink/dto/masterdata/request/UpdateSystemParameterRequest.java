package com.fa26se088.filmlink.dto.masterdata.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request cập nhật giá trị tham số hệ thống từ Admin (UC54).
 * Có version để kiểm tra Optimistic Locking chống xung đột ghi đè.
 */
public record UpdateSystemParameterRequest(
    @NotBlank(message = "Giá trị tham số không được để trống")
    @Size(max = 255, message = "Giá trị tham số không được vượt quá 255 ký tự")
    String value,

    @NotNull(message = "Version không được để trống")
    Integer version) {}
