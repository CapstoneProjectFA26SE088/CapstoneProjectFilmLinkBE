package com.fa26se088.filmlink.dto.delivery;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// class dùng để trả dữ liệu từ API của GHN, code , message và data
@JsonIgnoreProperties(ignoreUnknown = true)
public record GhnEnvelope<T>(
    int code,
    String message,
    T data
) {}
