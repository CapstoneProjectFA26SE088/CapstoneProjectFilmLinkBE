package com.fa26se088.filmlink.mapper.masterdata;

import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import com.fa26se088.filmlink.entity.masterdata.FilmFormat;
import com.fa26se088.filmlink.entity.masterdata.FilmProcess;
import com.fa26se088.filmlink.entity.masterdata.FilmStock;
import com.fa26se088.filmlink.entity.masterdata.ServiceType;
import org.springframework.stereotype.Component;

/** Chuyển entity master data sang DTO. Viết tay, không dùng MapStruct. */
@Component
public class MasterdataMapper {

  public FilmProcessResponse toResponse(FilmProcess process) {
    if (process == null) {
      return null;
    }
    return new FilmProcessResponse(
        process.getId(),
        process.getCode(),
        process.getDisplayName(),
        process.getDescription(),
        process.getStatus());
  }

  public FilmFormatResponse toResponse(FilmFormat format) {
    if (format == null) {
      return null;
    }
    return new FilmFormatResponse(
        format.getId(),
        format.getCode(),
        format.getDisplayName(),
        format.getDescription(),
        format.getStatus());
  }

  public ServiceTypeResponse toResponse(ServiceType serviceType) {
    if (serviceType == null) {
      return null;
    }
    return new ServiceTypeResponse(
        serviceType.getId(),
        serviceType.getCode(),
        serviceType.getDisplayName(),
        serviceType.getDescription(),
        serviceType.getStatus());
  }

  public FilmStockResponse toResponse(FilmStock stock) {
    if (stock == null) {
      return null;
    }
    Long processId = stock.getDefaultFilmProcess() != null ? stock.getDefaultFilmProcess().getId() : null;
    String processName =
        stock.getDefaultFilmProcess() != null ? stock.getDefaultFilmProcess().getDisplayName() : null;
    return new FilmStockResponse(
        stock.getId(),
        stock.getStockName(),
        stock.getManufacturer(),
        stock.getIso(),
        processId,
        processName,
        stock.getStatus());
  }

  public com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse toResponse(
      com.fa26se088.filmlink.entity.masterdata.SystemParameter param) {
    if (param == null) {
      return null;
    }
    Long updatedByAccountId =
        param.getUpdatedBy() != null ? param.getUpdatedBy().getId() : null;
    return new com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse(
        param.getId(),
        param.getKey(),
        param.getValue(),
        param.getValueType(),
        param.getDescription(),
        param.getVersion(),
        updatedByAccountId,
        param.getUpdatedAt());
  }
}
