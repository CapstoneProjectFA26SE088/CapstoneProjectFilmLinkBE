package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục dòng phim thương mại (MD-01, UC53, BR-16). */
@RestController
@RequestMapping("/api/v1/film-stocks")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, UC53, BR-16)")
public class FilmStockController {

  private final MasterdataService masterdataService;

  @GetMapping
  @Operation(
      summary = "Lấy danh sách dòng phim thương mại (MD-01)",
      description =
          "API công khai (No-Auth), chỉ trả về các dòng phim ACTIVE. Hỗ trợ tìm kiếm theo tên dòng phim hoặc tên hãng.")
  public List<FilmStockResponse> getFilmStocks(
      @Parameter(description = "Từ khoá tìm kiếm theo tên phim hoặc hãng (ví dụ: Gold, Kodak, Cinestill)")
          @RequestParam(name = "q", required = false)
          String query) {
    return masterdataService.getFilmStocks(query);
  }
}
