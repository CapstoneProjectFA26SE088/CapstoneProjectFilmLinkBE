package com.fa26se088.filmlink.service.masterdata.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.entity.masterdata.FilmFormat;
import com.fa26se088.filmlink.entity.masterdata.FilmProcess;
import com.fa26se088.filmlink.entity.masterdata.FilmStock;
import com.fa26se088.filmlink.entity.masterdata.ServiceType;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import com.fa26se088.filmlink.mapper.masterdata.MasterdataMapper;
import com.fa26se088.filmlink.repository.masterdata.FilmFormatRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmProcessRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmStockRepository;
import com.fa26se088.filmlink.repository.masterdata.ServiceTypeRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterdataServiceImplTest {

  @Mock private FilmProcessRepository filmProcessRepository;
  @Mock private FilmFormatRepository filmFormatRepository;
  @Mock private ServiceTypeRepository serviceTypeRepository;
  @Mock private FilmStockRepository filmStockRepository;

  private MasterdataMapper masterdataMapper;
  private MasterdataServiceImpl masterdataService;

  @BeforeEach
  void setUp() {
    masterdataMapper = new MasterdataMapper();
    masterdataService =
        new MasterdataServiceImpl(
            filmProcessRepository,
            filmFormatRepository,
            serviceTypeRepository,
            filmStockRepository,
            masterdataMapper);
  }

  @Test
  @DisplayName("getFilmProcesses chỉ lấy các quy trình ACTIVE")
  void getFilmProcesses_returnsActiveProcessesOnly() {
    FilmProcess process = new FilmProcess();
    process.setCode("C41");
    process.setDisplayName("C-41");
    process.setDescription("Color negative");
    process.setStatus(CatalogStatus.ACTIVE);

    when(filmProcessRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE))
        .thenReturn(List.of(process));

    List<FilmProcessResponse> result = masterdataService.getFilmProcesses();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().code()).isEqualTo("C41");
    assertThat(result.getFirst().displayName()).isEqualTo("C-41");
    assertThat(result.getFirst().status()).isEqualTo(CatalogStatus.ACTIVE);
    verify(filmProcessRepository).findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE);
  }

  @Test
  @DisplayName("getFilmFormats chỉ lấy các khổ phim ACTIVE")
  void getFilmFormats_returnsActiveFormatsOnly() {
    FilmFormat format = new FilmFormat();
    format.setCode("135");
    format.setDisplayName("35mm");
    format.setStatus(CatalogStatus.ACTIVE);

    when(filmFormatRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE))
        .thenReturn(List.of(format));

    List<FilmFormatResponse> result = masterdataService.getFilmFormats();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().code()).isEqualTo("135");
    assertThat(result.getFirst().displayName()).isEqualTo("35mm");
    assertThat(result.getFirst().status()).isEqualTo(CatalogStatus.ACTIVE);
  }

  @Test
  @DisplayName("getServiceTypes chỉ lấy các loại dịch vụ ACTIVE")
  void getServiceTypes_returnsActiveServiceTypesOnly() {
    ServiceType serviceType = new ServiceType();
    serviceType.setCode("DEVELOPING");
    serviceType.setDisplayName("Tráng phim");
    serviceType.setStatus(CatalogStatus.ACTIVE);

    when(serviceTypeRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE))
        .thenReturn(List.of(serviceType));

    List<ServiceTypeResponse> result = masterdataService.getServiceTypes();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().code()).isEqualTo("DEVELOPING");
    assertThat(result.getFirst().displayName()).isEqualTo("Tráng phim");
  }

  @Test
  @DisplayName("getFilmStocks tìm kiếm theo query và chỉ lấy ACTIVE")
  void getFilmStocks_returnsFilteredActiveStocks() {
    FilmProcess process = new FilmProcess();
    process.setCode("C41");
    process.setDisplayName("C-41");

    FilmStock stock = new FilmStock();
    stock.setStockName("Gold 200");
    stock.setManufacturer("Kodak");
    stock.setIso((short) 200);
    stock.setDefaultFilmProcess(process);
    stock.setStatus(CatalogStatus.ACTIVE);

    when(filmStockRepository.searchActiveFilmStocks(CatalogStatus.ACTIVE, "kodak"))
        .thenReturn(List.of(stock));

    List<FilmStockResponse> result = masterdataService.getFilmStocks("  kodak  ");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().stockName()).isEqualTo("Gold 200");
    assertThat(result.getFirst().manufacturer()).isEqualTo("Kodak");
    assertThat(result.getFirst().defaultFilmProcessName()).isEqualTo("C-41");
    verify(filmStockRepository).searchActiveFilmStocks(CatalogStatus.ACTIVE, "kodak");
  }

  @Test
  @DisplayName("getFilmStocks query null hoặc rỗng thì truyền query null xuống repository")
  void getFilmStocks_blankQuery_passesNull() {
    when(filmStockRepository.searchActiveFilmStocks(CatalogStatus.ACTIVE, null))
        .thenReturn(List.of());

    List<FilmStockResponse> result = masterdataService.getFilmStocks("   ");

    assertThat(result).isEmpty();
    verify(filmStockRepository).searchActiveFilmStocks(CatalogStatus.ACTIVE, null);
  }
}
