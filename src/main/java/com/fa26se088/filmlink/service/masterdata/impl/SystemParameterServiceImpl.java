package com.fa26se088.filmlink.service.masterdata.impl;

import com.fa26se088.filmlink.config.CacheConfig;
import com.fa26se088.filmlink.entity.masterdata.SystemParameter;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.masterdata.SystemParameterRepository;
import com.fa26se088.filmlink.service.masterdata.SystemParameterService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

  private final SystemParameterRepository systemParameterRepository;

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, key = "#key.name() + '_int'")
  public Integer getInteger(SystemParameterKey key) {
    String raw = findParam(key).getValue();
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ex) {
      log.error("Tham số {} có giá trị '{}' không thể parse thành Integer", key, raw);
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Tham số " + key.name() + " có giá trị không hợp lệ: " + raw);
    }
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, key = "#key.name() + '_percent'")
  public Double getPercent(SystemParameterKey key) {
    String raw = findParam(key).getValue();
    try {
      return Double.parseDouble(raw.trim());
    } catch (NumberFormatException ex) {
      log.error("Tham số {} có giá trị '{}' không thể parse thành Double/Percent", key, raw);
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Tham số " + key.name() + " có giá trị tỉ lệ phần trăm không hợp lệ: " + raw);
    }
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, key = "#key.name() + '_decimal'")
  public BigDecimal getDecimal(SystemParameterKey key) {
    String raw = findParam(key).getValue();
    try {
      return new BigDecimal(raw.trim());
    } catch (NumberFormatException ex) {
      log.error("Tham số {} có giá trị '{}' không thể parse thành BigDecimal", key, raw);
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Tham số " + key.name() + " có giá trị thập phân không hợp lệ: " + raw);
    }
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, key = "#key.name() + '_str'")
  public String getString(SystemParameterKey key) {
    return findParam(key).getValue();
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, key = "#key.name()")
  public SystemParameter getParameter(SystemParameterKey key) {
    return findParam(key);
  }

  private SystemParameter findParam(SystemParameterKey key) {
    return systemParameterRepository
        .findByKey(key)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.SYSTEM_PARAMETER_NOT_FOUND,
                    "Không tìm thấy tham số hệ thống: " + key.name()));
  }
}
