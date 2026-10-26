package com.fa26se088.filmlink.service.masterdata.impl;

import com.fa26se088.filmlink.config.CacheConfig;
import com.fa26se088.filmlink.dto.audit.request.AuditEntry;
import com.fa26se088.filmlink.dto.masterdata.request.UpdateSystemParameterRequest;
import com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.audit.enums.AuditAction;
import com.fa26se088.filmlink.entity.audit.enums.AuditTargetType;
import com.fa26se088.filmlink.entity.masterdata.SystemParameter;
import com.fa26se088.filmlink.entity.masterdata.enums.ParameterValueType;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.mapper.masterdata.MasterdataMapper;
import com.fa26se088.filmlink.repository.account.UserAccountRepository;
import com.fa26se088.filmlink.repository.masterdata.SystemParameterRepository;
import com.fa26se088.filmlink.service.audit.AuditLogService;
import com.fa26se088.filmlink.service.masterdata.SystemParameterAdminService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemParameterAdminServiceImpl implements SystemParameterAdminService {

  private final SystemParameterRepository systemParameterRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogService auditLogService;
  private final MasterdataMapper masterdataMapper;

  @Override
  @Transactional(readOnly = true)
  public List<SystemParameterResponse> getAllParameters() {
    return systemParameterRepository.findAllByOrderByKeyAsc().stream()
        .map(masterdataMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SystemParameterResponse getParameter(SystemParameterKey key) {
    SystemParameter entity = findEntity(key);
    return masterdataMapper.toResponse(entity);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheConfig.CACHE_SYSTEM_PARAMETERS, allEntries = true)
  public SystemParameterResponse updateParameter(
      SystemParameterKey key, UpdateSystemParameterRequest request, Long adminAccountId) {
    SystemParameter entity = findEntity(key);

    // 1. Kiểm tra Optimistic Locking
    if (!entity.getVersion().equals(request.version())) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_CONFLICT,
          "Tham số hệ thống đã được cập nhật bởi một Admin khác. Vui lòng tải lại trang và thử lại.",
          Map.of(
              "currentVersion", entity.getVersion(),
              "submittedVersion", request.version()));
    }

    String newValue = request.value().trim();

    // 2. Kiểm tra miền giá trị và các ràng buộc nghiệp vụ (BR-38)
    validateParameterValue(key, entity.getValueType(), newValue);

    // 3. Snapshot dữ liệu trước thay đổi
    Map<String, Object> before = toSnapshot(entity);

    // 4. Cập nhật dữ liệu
    entity.setValue(newValue);
    if (adminAccountId != null) {
      UserAccount admin = userAccountRepository.findById(adminAccountId).orElse(null);
      entity.setUpdatedBy(admin);
    }
    systemParameterRepository.saveAndFlush(entity);

    // 5. Snapshot dữ liệu sau thay đổi
    Map<String, Object> after = toSnapshot(entity);

    // 6. Ghi Audit Log thành công
    auditLogService.recordSuccess(
        new AuditEntry(
            AuditAction.SYSTEM_PARAMETER_UPDATED,
            AuditTargetType.SYSTEM_PARAMETER,
            entity.getId(),
            null,
            before,
            after));

    return masterdataMapper.toResponse(entity);
  }

  private void validateParameterValue(
      SystemParameterKey key, ParameterValueType valueType, String value) {
    switch (valueType) {
      case INTEGER -> validateIntegerParameter(key, value);
      case PERCENT -> validatePercentParameter(key, value);
      case DECIMAL -> validateDecimalParameter(key, value);
      case STRING -> validateStringParameter(key, value);
    }
  }

  private void validateIntegerParameter(SystemParameterKey key, String value) {
    int intVal;
    try {
      intVal = Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Giá trị tham số " + key.name() + " phải là số nguyên hợp lệ.");
    }

    if (key == SystemParameterKey.BILLING_DUE_DAY_OF_MONTH) {
      if (intVal < 1 || intVal > 28) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Ngày đến hạn thanh toán công nợ phải nằm trong khoảng từ ngày 1 đến ngày 28 (BR-20).");
      }
    } else if (key == SystemParameterKey.SANCTION_LEVEL_1_DAYS
        || key == SystemParameterKey.SANCTION_LEVEL_2_DAYS
        || key == SystemParameterKey.SANCTION_LEVEL_3_DAYS) {
      if (intVal <= 0) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Số ngày áp dụng chế tài phải là số nguyên dương (> 0).");
      }
      validateSanctionLevelsOrder(key, intVal);
    } else if (key == SystemParameterKey.FREE_STORAGE_QUOTA_GB
        || key == SystemParameterKey.CHATBOT_DAILY_QUESTION_QUOTA) {
      if (intVal < 0) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Hạn mức tham số " + key.name() + " không được là số âm.");
      }
    } else {
      if (intVal <= 0) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Thời hạn tham số " + key.name() + " phải là số nguyên dương (> 0).");
      }
    }
  }

  private void validateSanctionLevelsOrder(SystemParameterKey currentKey, int newIntValue) {
    int s1 =
        (currentKey == SystemParameterKey.SANCTION_LEVEL_1_DAYS)
            ? newIntValue
            : getExistingIntValue(SystemParameterKey.SANCTION_LEVEL_1_DAYS);
    int s2 =
        (currentKey == SystemParameterKey.SANCTION_LEVEL_2_DAYS)
            ? newIntValue
            : getExistingIntValue(SystemParameterKey.SANCTION_LEVEL_2_DAYS);
    int s3 =
        (currentKey == SystemParameterKey.SANCTION_LEVEL_3_DAYS)
            ? newIntValue
            : getExistingIntValue(SystemParameterKey.SANCTION_LEVEL_3_DAYS);

    if (s1 >= s2 || s2 >= s3) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          String.format(
              "Ngưỡng chế tài phải tăng dần nghiêm ngặt: Mức 1 < Mức 2 < Mức 3 (giá trị dự kiến: %d < %d < %d).",
              s1, s2, s3));
    }
  }

  private int getExistingIntValue(SystemParameterKey key) {
    SystemParameter param = findEntity(key);
    try {
      return Integer.parseInt(param.getValue().trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void validatePercentParameter(SystemParameterKey key, String value) {
    double percentVal;
    try {
      percentVal = Double.parseDouble(value);
    } catch (NumberFormatException ex) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Giá trị tham số tỉ lệ phần trăm phải là số hợp lệ.");
    }

    if (key == SystemParameterKey.DEFAULT_COMMISSION_RATE) {
      if (percentVal < 0 || percentVal > 30) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Tỉ lệ hoa hồng mặc định của sàn phải nằm trong khoảng từ 0% đến 30% (BR-10).");
      }
    } else if (key == SystemParameterKey.ORDER_CANCELLATION_FEE_RATE) {
      if (percentVal < 0 || percentVal > 100) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Tỉ lệ phí hủy đơn phải nằm trong khoảng từ 0% đến 100% (BR-38).");
      }
    } else {
      if (percentVal < 0 || percentVal > 100) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Tỉ lệ phần trăm phải nằm trong khoảng từ 0% đến 100%.");
      }
    }
  }

  private void validateDecimalParameter(SystemParameterKey key, String value) {
    try {
      BigDecimal decimalVal = new BigDecimal(value);
      if (decimalVal.compareTo(BigDecimal.ZERO) < 0) {
        throw new BusinessException(
            ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
            "Giá trị tham số " + key.name() + " không được là số âm.");
      }
    } catch (NumberFormatException ex) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Giá trị tham số " + key.name() + " phải là số thập phân hợp lệ.");
    }
  }

  private void validateStringParameter(SystemParameterKey key, String value) {
    if (value.isBlank()) {
      throw new BusinessException(
          ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE,
          "Giá trị tham số " + key.name() + " không được để trống.");
    }
  }

  private Map<String, Object> toSnapshot(SystemParameter entity) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("key", entity.getKey().name());
    snapshot.put("value", entity.getValue());
    snapshot.put("valueType", entity.getValueType().name());
    snapshot.put("version", entity.getVersion());
    if (entity.getUpdatedBy() != null) {
      snapshot.put("updatedByAccountId", entity.getUpdatedBy().getId());
    }
    return snapshot;
  }

  private SystemParameter findEntity(SystemParameterKey key) {
    return systemParameterRepository
        .findByKey(key)
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.SYSTEM_PARAMETER_NOT_FOUND,
                    "Không tìm thấy tham số hệ thống: " + key.name()));
  }
}
