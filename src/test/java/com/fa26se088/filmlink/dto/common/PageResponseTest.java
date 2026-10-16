package com.fa26se088.filmlink.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

  /** CR-01: trang 0, size 20, 45 bản ghi => 3 trang, còn trang sau. */
  @Test
  void from_copiesPagingMetadata() {
    Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 45);

    PageResponse<String> response = PageResponse.from(page);

    assertThat(response.content()).containsExactly("a", "b");
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.totalElements()).isEqualTo(45);
    assertThat(response.totalPages()).isEqualTo(3);
    assertThat(response.hasNext()).isTrue();
  }

  @Test
  void from_withMapper_mapsContentAndKeepsMetadata() {
    Page<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(2, 2), 6);

    PageResponse<String> response = PageResponse.from(page, value -> "#" + value);

    assertThat(response.content()).containsExactly("#1", "#2");
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(6);
    assertThat(response.totalPages()).isEqualTo(3);
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  void from_emptyPage_returnsEmptyContent() {
    Page<String> page = Page.empty(PageRequest.of(0, 20));

    PageResponse<String> response = PageResponse.from(page);

    assertThat(response.content()).isEmpty();
    assertThat(response.totalElements()).isZero();
    assertThat(response.hasNext()).isFalse();
  }
}
