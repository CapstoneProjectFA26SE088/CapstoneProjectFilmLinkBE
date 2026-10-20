package com.fa26se088.filmlink.service.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.CreateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.CreateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import java.util.List;

/**
 * Quản trị danh mục master data (MD-02, UC53, BR-16, BR-32).
 *
 * <p>
 * Admin-only: Create, Update, Toggle trạng thái (ACTIVE ↔ DISCONTINUED),
 * Soft Delete (chặn xóa cứng nếu có tham chiếu). Mọi thao tác đều ghi audit
 * log.
 */
public interface MasterdataAdminService {

  // ---- FilmProcess ----

  List<FilmProcessResponse> getAllFilmProcesses();

  FilmProcessResponse createFilmProcess(CreateCatalogRequest request);

  FilmProcessResponse updateFilmProcess(Long id, UpdateCatalogRequest request);

  FilmProcessResponse toggleFilmProcessStatus(Long id);

  void deleteFilmProcess(Long id);

  // ---- FilmFormat ----

  List<FilmFormatResponse> getAllFilmFormats();

  FilmFormatResponse createFilmFormat(CreateCatalogRequest request);

  FilmFormatResponse updateFilmFormat(Long id, UpdateCatalogRequest request);

  FilmFormatResponse toggleFilmFormatStatus(Long id);

  void deleteFilmFormat(Long id);

  // ---- ServiceType ----

  List<ServiceTypeResponse> getAllServiceTypes();

  ServiceTypeResponse createServiceType(CreateCatalogRequest request);

  ServiceTypeResponse updateServiceType(Long id, UpdateCatalogRequest request);

  ServiceTypeResponse toggleServiceTypeStatus(Long id);

  void deleteServiceType(Long id);

  // ---- FilmStock ----

  List<FilmStockResponse> getAllFilmStocks(String query);

  FilmStockResponse createFilmStock(CreateFilmStockRequest request);

  FilmStockResponse updateFilmStock(Long id, UpdateFilmStockRequest request);

  FilmStockResponse toggleFilmStockStatus(Long id);

  void deleteFilmStock(Long id);
}
