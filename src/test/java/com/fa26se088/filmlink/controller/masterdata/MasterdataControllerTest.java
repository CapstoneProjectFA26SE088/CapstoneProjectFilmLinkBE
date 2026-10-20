package com.fa26se088.filmlink.controller.masterdata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Kiểm tra 4 endpoint master data công khai (No-Auth) · MD-01, BR-16. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterdataControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MasterdataService masterdataService;

  @Test
  @DisplayName("GET /api/v1/film-processes công khai, trả 200 OK mà không cần đăng nhập")
  void getFilmProcesses_unauthenticated_returns200() throws Exception {
    when(masterdataService.getFilmProcesses())
        .thenReturn(
            List.of(
                new FilmProcessResponse(
                    1L, "C41", "C-41", "Color negative", CatalogStatus.ACTIVE)));

    mockMvc
        .perform(get("/api/v1/film-processes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("C41"))
        .andExpect(jsonPath("$[0].displayName").value("C-41"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  @DisplayName("GET /api/v1/film-formats công khai, trả 200 OK mà không cần đăng nhập")
  void getFilmFormats_unauthenticated_returns200() throws Exception {
    when(masterdataService.getFilmFormats())
        .thenReturn(List.of(new FilmFormatResponse(1L, "135", "35mm", null, CatalogStatus.ACTIVE)));

    mockMvc
        .perform(get("/api/v1/film-formats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("135"))
        .andExpect(jsonPath("$[0].displayName").value("35mm"));
  }

  @Test
  @DisplayName("GET /api/v1/service-types công khai, trả 200 OK mà không cần đăng nhập")
  void getServiceTypes_unauthenticated_returns200() throws Exception {
    when(masterdataService.getServiceTypes())
        .thenReturn(
            List.of(
                new ServiceTypeResponse(
                    1L, "DEVELOPING", "Tráng phim", null, CatalogStatus.ACTIVE)));

    mockMvc
        .perform(get("/api/v1/service-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("DEVELOPING"));
  }

  @Test
  @DisplayName("GET /api/v1/film-stocks công khai, có hỗ trợ query param ?q=")
  void getFilmStocks_withQuery_returns200() throws Exception {
    when(masterdataService.getFilmStocks("kodak"))
        .thenReturn(
            List.of(
                new FilmStockResponse(
                    1L, "Gold 200", "Kodak", (short) 200, 1L, "C-41", CatalogStatus.ACTIVE)));

    mockMvc
        .perform(get("/api/v1/film-stocks").param("q", "kodak"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockName").value("Gold 200"))
        .andExpect(jsonPath("$[0].manufacturer").value("Kodak"));

    verify(masterdataService).getFilmStocks("kodak");
  }
}
