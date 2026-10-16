package com.fa26se088.filmlink.dto.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Một trang kết quả trả cho client (CR-01, CR-02).
 *
 * <p>Không trả thẳng {@link Page} / {@code PageImpl} ra JSON vì cấu trúc JSON của chúng không ổn
 * định giữa các bản Spring Data. Trang đánh số từ 0.
 *
 * @param content dữ liệu của trang hiện tại
 * @param page số trang hiện tại, bắt đầu từ 0
 * @param size kích thước trang được yêu cầu
 * @param totalElements tổng số bản ghi khớp điều kiện
 * @param totalPages tổng số trang
 * @param hasNext còn trang sau hay không
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

  /** Dùng khi {@code Page} đã chứa sẵn response DTO. */
  public static <T> PageResponse<T> from(Page<T> page) {
    return from(page, Function.identity());
  }

  /** Dùng khi {@code Page} chứa entity: {@code mapper} chuyển từng phần tử sang response DTO. */
  public static <E, T> PageResponse<T> from(Page<E> page, Function<? super E, ? extends T> mapper) {
    List<T> content = page.getContent().stream().<T>map(mapper).toList();
    return new PageResponse<>(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext());
  }
}
