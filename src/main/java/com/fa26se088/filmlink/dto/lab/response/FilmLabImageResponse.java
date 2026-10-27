package com.fa26se088.filmlink.dto.lab.response;

import java.net.URI;

/**
 * Ảnh mẫu của lab. {@code url} là link ký có hạn (NFR-PRI-02), tạo lúc trả response, không lưu DB.
 */
public record FilmLabImageResponse(Long id, Short displayOrder, URI url) {}
