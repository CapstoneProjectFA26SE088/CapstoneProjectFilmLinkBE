package com.fa26se088.filmlink.controller.masterdata;

import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục quy trình hóa chất ngành phim (MD-01, UC53, BR-16). */
@RestController
@RequestMapping("/api/v1/film-processes")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục thông số kỹ thuật ngành phim (MD-01, UC53, BR-16)")
public class FilmProcessController {

  private final MasterdataService masterdataService;

  @GetMapping
  @Operation(
      summary = "Lấy danh sách quy trình hóa chất (MD-01)",
      description = "API công khai (No-Auth), chỉ trả về các quy trình ACTIVE (C-41, B&W, E-6...).")
  public List<FilmProcessResponse> getFilmProcesses() {
    return masterdataService.getFilmProcesses();
  }
}
