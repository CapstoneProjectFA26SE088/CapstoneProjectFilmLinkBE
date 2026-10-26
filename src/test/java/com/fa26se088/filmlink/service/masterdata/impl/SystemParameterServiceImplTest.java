package com.fa26se088.filmlink.service.masterdata.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fa26se088.filmlink.entity.masterdata.SystemParameter;
import com.fa26se088.filmlink.entity.masterdata.enums.ParameterValueType;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import com.fa26se088.filmlink.repository.masterdata.SystemParameterRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemParameterServiceImplTest {

  @Mock private SystemParameterRepository systemParameterRepository;

  private SystemParameterServiceImpl systemParameterService;

  @BeforeEach
  void setUp() {
    systemParameterService = new SystemParameterServiceImpl(systemParameterRepository);
  }

  @Test
  @DisplayName("getInteger đọc đúng số nguyên từ DB")
  void getInteger_returnsParsedInteger() {
    SystemParameter param = new SystemParameter();
    param.setKey(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH);
    param.setValue("10");
    param.setValueType(ParameterValueType.INTEGER);

    when(systemParameterRepository.findByKey(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH))
        .thenReturn(Optional.of(param));

    Integer result = systemParameterService.getInteger(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH);
    assertThat(result).isEqualTo(10);
  }

  @Test
  @DisplayName("getInteger ném SYSTEM_PARAMETER_INVALID_VALUE khi giá trị không phải số nguyên")
  void getInteger_invalidFormat_throwsBusinessException() {
    SystemParameter param = new SystemParameter();
    param.setKey(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH);
    param.setValue("abc");
    param.setValueType(ParameterValueType.INTEGER);

    when(systemParameterRepository.findByKey(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH))
        .thenReturn(Optional.of(param));

    assertThatThrownBy(
            () -> systemParameterService.getInteger(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE);
  }

  @Test
  @DisplayName("getPercent đọc đúng tỉ lệ phần trăm dạng Double")
  void getPercent_returnsParsedDouble() {
    SystemParameter param = new SystemParameter();
    param.setKey(SystemParameterKey.DEFAULT_COMMISSION_RATE);
    param.setValue("10.5");
    param.setValueType(ParameterValueType.PERCENT);

    when(systemParameterRepository.findByKey(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(Optional.of(param));

    Double result = systemParameterService.getPercent(SystemParameterKey.DEFAULT_COMMISSION_RATE);
    assertThat(result).isEqualTo(10.5);
  }

  @Test
  @DisplayName("getDecimal đọc đúng dạng BigDecimal")
  void getDecimal_returnsParsedBigDecimal() {
    SystemParameter param = new SystemParameter();
    param.setKey(SystemParameterKey.DEFAULT_COMMISSION_RATE);
    param.setValue("15.25");
    param.setValueType(ParameterValueType.PERCENT);

    when(systemParameterRepository.findByKey(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(Optional.of(param));

    BigDecimal result = systemParameterService.getDecimal(SystemParameterKey.DEFAULT_COMMISSION_RATE);
    assertThat(result).isEqualByComparingTo("15.25");
  }

  @Test
  @DisplayName("getString đọc đúng chuỗi String")
  void getString_returnsValue() {
    SystemParameter param = new SystemParameter();
    param.setKey(SystemParameterKey.ORDER_AUTO_CLOSE_DAYS);
    param.setValue("7");
    param.setValueType(ParameterValueType.INTEGER);

    when(systemParameterRepository.findByKey(SystemParameterKey.ORDER_AUTO_CLOSE_DAYS))
        .thenReturn(Optional.of(param));

    String result = systemParameterService.getString(SystemParameterKey.ORDER_AUTO_CLOSE_DAYS);
    assertThat(result).isEqualTo("7");
  }

  @Test
  @DisplayName("getParameter ném SYSTEM_PARAMETER_NOT_FOUND khi tham số không tồn tại")
  void getParameter_notFound_throwsBusinessException() {
    when(systemParameterRepository.findByKey(SystemParameterKey.FREE_STORAGE_QUOTA_GB))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> systemParameterService.getParameter(SystemParameterKey.FREE_STORAGE_QUOTA_GB))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_NOT_FOUND);
  }
}
