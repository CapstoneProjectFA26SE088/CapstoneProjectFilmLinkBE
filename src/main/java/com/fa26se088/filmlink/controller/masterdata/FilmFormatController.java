package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục khổ phim ngành phim (MD-01, UC53, BR-16). */
@RestController
@RequestMapping("/api/v1/film-formats")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, UC53, BR-16)")
public class FilmFormatController {

  private final MasterdataService masterdataService;

  @GetMapping
  @Operation(
      summary = "Lấy danh sách khổ phim (MD-01)",
      description = "API công khai (No-Auth), chỉ trả về các khổ phim ACTIVE (35mm, 120, Sheet...).")
  public List<FilmFormatResponse> getFilmFormats() {
    return masterdataService.getFilmFormats();
  }
}
