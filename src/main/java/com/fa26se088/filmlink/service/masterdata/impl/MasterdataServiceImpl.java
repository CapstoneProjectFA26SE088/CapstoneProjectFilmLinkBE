package com.fa26se088.filmlink.service.masterdata.impl;

import com.fa26se088.filmlink.config.CacheConfig;
import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import com.fa26se088.filmlink.mapper.masterdata.MasterdataMapper;
import com.fa26se088.filmlink.repository.masterdata.FilmFormatRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmProcessRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmStockRepository;
import com.fa26se088.filmlink.repository.masterdata.ServiceTypeRepository;
import com.fa26se088.filmlink.service.masterdata.MasterdataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai nghiệp vụ đọc danh mục master data (MD-01, UC53, BR-16).
 *
 * <p>Bắt buộc chỉ truy vấn dữ liệu {@code status = ACTIVE} và cache kết quả 24h.
 */
@Service
@RequiredArgsConstructor
public class MasterdataServiceImpl implements MasterdataService {

  private final FilmProcessRepository filmProcessRepository;
  private final FilmFormatRepository filmFormatRepository;
  private final ServiceTypeRepository serviceTypeRepository;
  private final FilmStockRepository filmStockRepository;
  private final MasterdataMapper masterdataMapper;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_FILM_PROCESSES)
  public List<FilmProcessResponse> getFilmProcesses() {
    return filmProcessRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE).stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_FILM_FORMATS)
  public List<FilmFormatResponse> getFilmFormats() {
    return filmFormatRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE).stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SERVICE_TYPES)
  public List<ServiceTypeResponse> getServiceTypes() {
    return serviceTypeRepository.findByStatusOrderByDisplayNameAsc(CatalogStatus.ACTIVE).stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = CacheConfig.CACHE_FILM_STOCKS,
      key = "#query != null ? #query.trim().toLowerCase() : ''")
  public List<FilmStockResponse> getFilmStocks(String query) {
    String normalizedQuery = (query != null && !query.isBlank()) ? query.trim() : null;
    return filmStockRepository.searchActiveFilmStocks(CatalogStatus.ACTIVE, normalizedQuery).stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }
}
