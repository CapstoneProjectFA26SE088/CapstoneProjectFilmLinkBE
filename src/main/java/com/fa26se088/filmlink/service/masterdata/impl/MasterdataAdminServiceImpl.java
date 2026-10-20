package com.fa26se088.filmlink.service.masterdata.impl;

import com.fa26se088.filmlink.config.CacheConfig;
import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.masterdata.request.CreateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.CreateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateCatalogRequest;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateFilmStockRequest;
import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.entity.masterdata.FilmFormat;
import com.fa26se088.filmlink.entity.masterdata.FilmProcess;
import com.fa26se088.filmlink.entity.masterdata.FilmStock;
import com.fa26se088.filmlink.entity.masterdata.ServiceType;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.mapper.masterdata.MasterdataMapper;
import com.fa26se088.filmlink.repository.masterdata.FilmFormatRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmProcessRepository;
import com.fa26se088.filmlink.repository.masterdata.FilmStockRepository;
import com.fa26se088.filmlink.repository.masterdata.ServiceTypeRepository;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import com.fa26se088.filmlink.service.masterdata.MasterdataAdminService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai nghiệp vụ quản trị danh mục master data (MD-02, UC53, BR-16, BR-32).
 *
 * <p>Các quy tắc:
 * <ul>
 *   <li>Code phải duy nhất (Unique) trên toàn bảng → 409 Conflict nếu trùng.</li>
 *   <li>Code bất biến (Immutable) → UpdateRequest không có trường code.</li>
 *   <li>Không xóa cứng khi có tham chiếu → chuyển sang DISCONTINUED.</li>
 *   <li>Mọi thao tác Create/Update/Toggle đều ghi audit log (BR-32).</li>
 *   <li>Mỗi thao tác ghi xong phải evict cache tương ứng (MD-01 cache 24h).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MasterdataAdminServiceImpl implements MasterdataAdminService {

  private final FilmProcessRepository filmProcessRepository;
  private final FilmFormatRepository filmFormatRepository;
  private final ServiceTypeRepository serviceTypeRepository;
  private final FilmStockRepository filmStockRepository;
  private final MasterdataMapper masterdataMapper;
  private final AuditLogService auditLogService;

  // ===================== FilmProcess =====================

  @Override
  @Transactional(readOnly = true)
  public List<FilmProcessResponse> getAllFilmProcesses() {
    return filmProcessRepository.findAllByOrderByDisplayNameAsc().stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_PROCESSES, allEntries = true)
  public FilmProcessResponse createFilmProcess(CreateCatalogRequest request) {
    if (filmProcessRepository.existsByCode(request.code())) {
      throw new BusinessException(
          ErrorCode.MASTERDATA_CODE_DUPLICATE,
          "Mã quy trình '" + request.code() + "' đã tồn tại.",
          Map.of("field", "code", "value", request.code()));
    }

    FilmProcess entity = new FilmProcess();
    entity.setCode(request.code());
    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    entity.setStatus(CatalogStatus.ACTIVE);
    filmProcessRepository.save(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_CREATED,
        AuditTargetType.FILM_PROCESS,
        entity.getId(),
        null,
        null,
        toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
            entity.getDescription(), entity.getStatus())));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_PROCESSES, allEntries = true)
  public FilmProcessResponse updateFilmProcess(Long id, UpdateCatalogRequest request) {
    FilmProcess entity = filmProcessRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy trình hóa chất với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    filmProcessRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_UPDATED, AuditTargetType.FILM_PROCESS,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_PROCESSES, allEntries = true)
  public FilmProcessResponse toggleFilmProcessStatus(Long id) {
    FilmProcess entity = filmProcessRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy trình hóa chất với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setStatus(toggleStatus(entity.getStatus()));
    filmProcessRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_PROCESS,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_PROCESSES, allEntries = true)
  public void deleteFilmProcess(Long id) {
    FilmProcess entity = filmProcessRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy trình hóa chất với id=" + id));

    // Integrity Check: FilmStock references FilmProcess via defaultFilmProcess
    boolean inUse = filmStockRepository.existsByDefaultFilmProcess(entity);

    if (inUse) {
      auditLogService.recordSuccess(new AuditEntry(
          AuditAction.MASTERDATA_DELETE_REJECTED, AuditTargetType.FILM_PROCESS,
          entity.getId(),
          "Đang được tham chiếu bởi film_stocks. Vui lòng chuyển sang DISCONTINUED.",
          toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
              entity.getDescription(), entity.getStatus()),
          null));

      throw new BusinessException(
          ErrorCode.MASTERDATA_IN_USE,
          "Quy trình '" + entity.getCode() + "' đang được sử dụng. "
              + "Không thể xóa, vui lòng chuyển trạng thái sang DISCONTINUED.");
    }

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    filmProcessRepository.delete(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_PROCESS,
        id, "Hard delete – không có tham chiếu", before, null));
  }

  // ===================== FilmFormat =====================

  @Override
  @Transactional(readOnly = true)
  public List<FilmFormatResponse> getAllFilmFormats() {
    return filmFormatRepository.findAllByOrderByDisplayNameAsc().stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_FORMATS, allEntries = true)
  public FilmFormatResponse createFilmFormat(CreateCatalogRequest request) {
    if (filmFormatRepository.existsByCode(request.code())) {
      throw new BusinessException(
          ErrorCode.MASTERDATA_CODE_DUPLICATE,
          "Mã khổ phim '" + request.code() + "' đã tồn tại.",
          Map.of("field", "code", "value", request.code()));
    }

    FilmFormat entity = new FilmFormat();
    entity.setCode(request.code());
    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    entity.setStatus(CatalogStatus.ACTIVE);
    filmFormatRepository.save(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_CREATED, AuditTargetType.FILM_FORMAT,
        entity.getId(), null, null,
        toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
            entity.getDescription(), entity.getStatus())));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_FORMATS, allEntries = true)
  public FilmFormatResponse updateFilmFormat(Long id, UpdateCatalogRequest request) {
    FilmFormat entity = filmFormatRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy khổ phim với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    filmFormatRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_UPDATED, AuditTargetType.FILM_FORMAT,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_FORMATS, allEntries = true)
  public FilmFormatResponse toggleFilmFormatStatus(Long id) {
    FilmFormat entity = filmFormatRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy khổ phim với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setStatus(toggleStatus(entity.getStatus()));
    filmFormatRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_FORMAT,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_FORMATS, allEntries = true)
  public void deleteFilmFormat(Long id) {
    FilmFormat entity = filmFormatRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy khổ phim với id=" + id));

    // TODO: Integrity Check khi bảng orders, lab_services, price_lists, lab_machines tồn tại
    boolean inUse = false;

    if (inUse) {
      auditLogService.recordSuccess(new AuditEntry(
          AuditAction.MASTERDATA_DELETE_REJECTED, AuditTargetType.FILM_FORMAT,
          entity.getId(),
          "Đang được tham chiếu. Vui lòng chuyển sang DISCONTINUED.",
          toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
              entity.getDescription(), entity.getStatus()),
          null));

      throw new BusinessException(
          ErrorCode.MASTERDATA_IN_USE,
          "Khổ phim '" + entity.getCode() + "' đang được sử dụng. "
              + "Không thể xóa, vui lòng chuyển trạng thái sang DISCONTINUED.");
    }

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    filmFormatRepository.delete(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_FORMAT,
        id, "Hard delete – không có tham chiếu", before, null));
  }

  // ===================== ServiceType =====================

  @Override
  @Transactional(readOnly = true)
  public List<ServiceTypeResponse> getAllServiceTypes() {
    return serviceTypeRepository.findAllByOrderByDisplayNameAsc().stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_SERVICE_TYPES, allEntries = true)
  public ServiceTypeResponse createServiceType(CreateCatalogRequest request) {
    if (serviceTypeRepository.existsByCode(request.code())) {
      throw new BusinessException(
          ErrorCode.MASTERDATA_CODE_DUPLICATE,
          "Mã loại dịch vụ '" + request.code() + "' đã tồn tại.",
          Map.of("field", "code", "value", request.code()));
    }

    ServiceType entity = new ServiceType();
    entity.setCode(request.code());
    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    entity.setStatus(CatalogStatus.ACTIVE);
    serviceTypeRepository.save(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_CREATED, AuditTargetType.SERVICE_TYPE,
        entity.getId(), null, null,
        toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
            entity.getDescription(), entity.getStatus())));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_SERVICE_TYPES, allEntries = true)
  public ServiceTypeResponse updateServiceType(Long id, UpdateCatalogRequest request) {
    ServiceType entity = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại dịch vụ với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setDisplayName(request.displayName());
    entity.setDescription(request.description());
    serviceTypeRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_UPDATED, AuditTargetType.SERVICE_TYPE,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_SERVICE_TYPES, allEntries = true)
  public ServiceTypeResponse toggleServiceTypeStatus(Long id) {
    ServiceType entity = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại dịch vụ với id=" + id));

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    entity.setStatus(toggleStatus(entity.getStatus()));
    serviceTypeRepository.save(entity);

    Map<String, Object> after = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.SERVICE_TYPE,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_SERVICE_TYPES, allEntries = true)
  public void deleteServiceType(Long id) {
    ServiceType entity = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại dịch vụ với id=" + id));

    // TODO: Integrity Check khi bảng orders, lab_services, price_lists, lab_machines tồn tại
    boolean inUse = false;

    if (inUse) {
      auditLogService.recordSuccess(new AuditEntry(
          AuditAction.MASTERDATA_DELETE_REJECTED, AuditTargetType.SERVICE_TYPE,
          entity.getId(),
          "Đang được tham chiếu. Vui lòng chuyển sang DISCONTINUED.",
          toCatalogSnapshot(entity.getCode(), entity.getDisplayName(),
              entity.getDescription(), entity.getStatus()),
          null));

      throw new BusinessException(
          ErrorCode.MASTERDATA_IN_USE,
          "Loại dịch vụ '" + entity.getCode() + "' đang được sử dụng. "
              + "Không thể xóa, vui lòng chuyển trạng thái sang DISCONTINUED.");
    }

    Map<String, Object> before = toCatalogSnapshot(
        entity.getCode(), entity.getDisplayName(), entity.getDescription(), entity.getStatus());

    serviceTypeRepository.delete(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.SERVICE_TYPE,
        id, "Hard delete – không có tham chiếu", before, null));
  }

  // ===================== FilmStock =====================

  @Override
  @Transactional(readOnly = true)
  public List<FilmStockResponse> getAllFilmStocks(String query) {
    return filmStockRepository.searchAllFilmStocks(query).stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_STOCKS, allEntries = true)
  public FilmStockResponse createFilmStock(CreateFilmStockRequest request) {
    if (filmStockRepository.existsByStockNameAndManufacturer(
        request.stockName(), request.manufacturer())) {
      throw new BusinessException(
          ErrorCode.MASTERDATA_CODE_DUPLICATE,
          "Dòng phim '" + request.stockName() + "' của hãng '"
              + request.manufacturer() + "' đã tồn tại.",
          Map.of("field", "stockName", "value", request.stockName()));
    }

    FilmProcess defaultProcess = filmProcessRepository.findById(request.defaultFilmProcessId())
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Không tìm thấy quy trình hóa chất với id=" + request.defaultFilmProcessId()));

    FilmStock entity = new FilmStock();
    entity.setStockName(request.stockName());
    entity.setManufacturer(request.manufacturer());
    entity.setIso(request.iso());
    entity.setDefaultFilmProcess(defaultProcess);
    entity.setStatus(CatalogStatus.ACTIVE);
    filmStockRepository.save(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_CREATED, AuditTargetType.FILM_STOCK,
        entity.getId(), null, null,
        toFilmStockSnapshot(entity)));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_STOCKS, allEntries = true)
  public FilmStockResponse updateFilmStock(Long id, UpdateFilmStockRequest request) {
    FilmStock entity = filmStockRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dòng phim với id=" + id));

    Map<String, Object> before = toFilmStockSnapshot(entity);

    FilmProcess defaultProcess = filmProcessRepository.findById(request.defaultFilmProcessId())
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Không tìm thấy quy trình hóa chất với id=" + request.defaultFilmProcessId()));

    entity.setStockName(request.stockName());
    entity.setManufacturer(request.manufacturer());
    entity.setIso(request.iso());
    entity.setDefaultFilmProcess(defaultProcess);
    filmStockRepository.save(entity);

    Map<String, Object> after = toFilmStockSnapshot(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_UPDATED, AuditTargetType.FILM_STOCK,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_STOCKS, allEntries = true)
  public FilmStockResponse toggleFilmStockStatus(Long id) {
    FilmStock entity = filmStockRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dòng phim với id=" + id));

    Map<String, Object> before = toFilmStockSnapshot(entity);

    entity.setStatus(toggleStatus(entity.getStatus()));
    filmStockRepository.save(entity);

    Map<String, Object> after = toFilmStockSnapshot(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_STOCK,
        entity.getId(), null, before, after));

    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_FILM_STOCKS, allEntries = true)
  public void deleteFilmStock(Long id) {
    FilmStock entity = filmStockRepository.findById(id)
        .orElseThrow(() -> new BusinessException(
            ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dòng phim với id=" + id));

    // TODO: Integrity Check khi bảng orders, price_lists tồn tại
    boolean inUse = false;

    if (inUse) {
      auditLogService.recordSuccess(new AuditEntry(
          AuditAction.MASTERDATA_DELETE_REJECTED, AuditTargetType.FILM_STOCK,
          entity.getId(),
          "Đang được tham chiếu. Vui lòng chuyển sang DISCONTINUED.",
          toFilmStockSnapshot(entity), null));

      throw new BusinessException(
          ErrorCode.MASTERDATA_IN_USE,
          "Dòng phim '" + entity.getStockName() + "' đang được sử dụng. "
              + "Không thể xóa, vui lòng chuyển trạng thái sang DISCONTINUED.");
    }

    Map<String, Object> before = toFilmStockSnapshot(entity);
    filmStockRepository.delete(entity);

    auditLogService.recordSuccess(new AuditEntry(
        AuditAction.MASTERDATA_STATUS_CHANGED, AuditTargetType.FILM_STOCK,
        id, "Hard delete – không có tham chiếu", before, null));
  }

  // ===================== Private helpers =====================

  private static CatalogStatus toggleStatus(CatalogStatus current) {
    return current == CatalogStatus.ACTIVE ? CatalogStatus.DISCONTINUED : CatalogStatus.ACTIVE;
  }

  /**
   * Snapshot dữ liệu danh mục 3-field cho audit log (before/after).
   * Dùng {@link LinkedHashMap} để giữ thứ tự key khi serialize JSON.
   */
  private static Map<String, Object> toCatalogSnapshot(
      String code, String displayName, String description, CatalogStatus status) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("code", code);
    snapshot.put("displayName", displayName);
    snapshot.put("description", description);
    snapshot.put("status", status.name());
    return snapshot;
  }

  /** Snapshot dữ liệu FilmStock cho audit log. */
  private static Map<String, Object> toFilmStockSnapshot(FilmStock entity) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("stockName", entity.getStockName());
    snapshot.put("manufacturer", entity.getManufacturer());
    snapshot.put("iso", entity.getIso());
    snapshot.put("defaultFilmProcessId",
        entity.getDefaultFilmProcess() != null ? entity.getDefaultFilmProcess().getId() : null);
    snapshot.put("status", entity.getStatus().name());
    return snapshot;
  }
}
