package com.fa26se088.filmlink.service.masterdata.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemParameterAdminServiceImplTest {

  @Mock private SystemParameterRepository systemParameterRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private AuditLogService auditLogService;

  private MasterdataMapper masterdataMapper;
  private SystemParameterAdminServiceImpl adminService;

  @BeforeEach
  void setUp() {
    masterdataMapper = new MasterdataMapper();
    adminService =
        new SystemParameterAdminServiceImpl(
            systemParameterRepository,
            userAccountRepository,
            auditLogService,
            masterdataMapper);
  }

  @Test
  @DisplayName("getAllParameters trả về toàn bộ danh sách đã map DTO")
  void getAllParameters_returnsMappedList() {
    SystemParameter p1 = createParam(SystemParameterKey.DEFAULT_COMMISSION_RATE, "10", ParameterValueType.PERCENT, 0);
    when(systemParameterRepository.findAllByOrderByKeyAsc()).thenReturn(List.of(p1));

    List<SystemParameterResponse> result = adminService.getAllParameters();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().key()).isEqualTo(SystemParameterKey.DEFAULT_COMMISSION_RATE);
    assertThat(result.getFirst().value()).isEqualTo("10");
  }

  @Test
  @DisplayName("updateParameter thành công: cập nhật giá trị, lưu DB và ghi audit log kèm snapshot before/after")
  void updateParameter_success_updatesAndAudits() {
    SystemParameter entity = createParam(SystemParameterKey.DEFAULT_COMMISSION_RATE, "10", ParameterValueType.PERCENT, 2);
    when(systemParameterRepository.findByKey(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(Optional.of(entity));

    UserAccount admin = new UserAccount();
    when(userAccountRepository.findById(99L)).thenReturn(Optional.of(admin));

    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("15", 2);
    SystemParameterResponse response =
        adminService.updateParameter(SystemParameterKey.DEFAULT_COMMISSION_RATE, request, 99L);

    assertThat(response.value()).isEqualTo("15");
    assertThat(entity.getValue()).isEqualTo("15");
    assertThat(entity.getUpdatedBy()).isEqualTo(admin);
    verify(systemParameterRepository).saveAndFlush(entity);

    ArgumentCaptor<AuditEntry> auditCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    verify(auditLogService).recordSuccess(auditCaptor.capture());
    AuditEntry auditEntry = auditCaptor.getValue();
    assertThat(auditEntry.action()).isEqualTo(AuditAction.SYSTEM_PARAMETER_UPDATED);
    assertThat(auditEntry.targetType()).isEqualTo(AuditTargetType.SYSTEM_PARAMETER);
    assertThat(auditEntry.before().get("value")).isEqualTo("10");
    assertThat(auditEntry.after().get("value")).isEqualTo("15");
  }

  @Test
  @DisplayName("updateParameter ném SYSTEM_PARAMETER_CONFLICT khi version không khớp (Optimistic Lock)")
  void updateParameter_versionMismatch_throwsConflict() {
    SystemParameter entity = createParam(SystemParameterKey.DEFAULT_COMMISSION_RATE, "10", ParameterValueType.PERCENT, 5);
    when(systemParameterRepository.findByKey(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(Optional.of(entity));

    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("15", 4);

    assertThatThrownBy(
            () ->
                adminService.updateParameter(
                    SystemParameterKey.DEFAULT_COMMISSION_RATE, request, 99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_CONFLICT);
  }

  @Test
  @DisplayName("updateParameter ném SYSTEM_PARAMETER_INVALID_VALUE khi hoa hồng > 30% (BR-10)")
  void updateParameter_commissionRateGreaterThan30_throwsInvalidValue() {
    SystemParameter entity = createParam(SystemParameterKey.DEFAULT_COMMISSION_RATE, "10", ParameterValueType.PERCENT, 0);
    when(systemParameterRepository.findByKey(SystemParameterKey.DEFAULT_COMMISSION_RATE))
        .thenReturn(Optional.of(entity));

    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("35", 0);

    assertThatThrownBy(
            () ->
                adminService.updateParameter(
                    SystemParameterKey.DEFAULT_COMMISSION_RATE, request, 99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE);
  }

  @Test
  @DisplayName("updateParameter ném SYSTEM_PARAMETER_INVALID_VALUE khi ngày đến hạn công nợ ngoài 1-28 (BR-20)")
  void updateParameter_billingDueDayInvalid_throwsInvalidValue() {
    SystemParameter entity = createParam(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH, "10", ParameterValueType.INTEGER, 0);
    when(systemParameterRepository.findByKey(SystemParameterKey.BILLING_DUE_DAY_OF_MONTH))
        .thenReturn(Optional.of(entity));

    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("31", 0);

    assertThatThrownBy(
            () ->
                adminService.updateParameter(
                    SystemParameterKey.BILLING_DUE_DAY_OF_MONTH, request, 99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE);
  }

  @Test
  @DisplayName("Ngưỡng chế tài không tăng dần bị chặn: Mức 1 >= Mức 2 ném SYSTEM_PARAMETER_INVALID_VALUE")
  void updateParameter_sanctionLevel1NotLessThanLevel2_throwsInvalidValue() {
    SystemParameter level1 = createParam(SystemParameterKey.SANCTION_LEVEL_1_DAYS, "1", ParameterValueType.INTEGER, 0);
    SystemParameter level2 = createParam(SystemParameterKey.SANCTION_LEVEL_2_DAYS, "15", ParameterValueType.INTEGER, 0);
    SystemParameter level3 = createParam(SystemParameterKey.SANCTION_LEVEL_3_DAYS, "30", ParameterValueType.INTEGER, 0);

    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_1_DAYS))
        .thenReturn(Optional.of(level1));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_2_DAYS))
        .thenReturn(Optional.of(level2));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_3_DAYS))
        .thenReturn(Optional.of(level3));

    // Thử cập nhật mức 1 thành 20 (lớn hơn mức 2 = 15)
    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("20", 0);

    assertThatThrownBy(
            () ->
                adminService.updateParameter(
                    SystemParameterKey.SANCTION_LEVEL_1_DAYS, request, 99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE);
  }

  @Test
  @DisplayName("Ngưỡng chế tài không tăng dần bị chặn: Mức 2 >= Mức 3 ném SYSTEM_PARAMETER_INVALID_VALUE")
  void updateParameter_sanctionLevel2NotLessThanLevel3_throwsInvalidValue() {
    SystemParameter level1 = createParam(SystemParameterKey.SANCTION_LEVEL_1_DAYS, "1", ParameterValueType.INTEGER, 0);
    SystemParameter level2 = createParam(SystemParameterKey.SANCTION_LEVEL_2_DAYS, "15", ParameterValueType.INTEGER, 0);
    SystemParameter level3 = createParam(SystemParameterKey.SANCTION_LEVEL_3_DAYS, "30", ParameterValueType.INTEGER, 0);

    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_2_DAYS))
        .thenReturn(Optional.of(level2));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_1_DAYS))
        .thenReturn(Optional.of(level1));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_3_DAYS))
        .thenReturn(Optional.of(level3));

    // Thử cập nhật mức 2 thành 30 (bằng mức 3 = 30)
    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("30", 0);

    assertThatThrownBy(
            () ->
                adminService.updateParameter(
                    SystemParameterKey.SANCTION_LEVEL_2_DAYS, request, 99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_PARAMETER_INVALID_VALUE);
  }

  @Test
  @DisplayName("Ngưỡng chế tài tăng dần hợp lệ: cho phép cập nhật thành công")
  void updateParameter_sanctionLevelValid_updatesSuccessfully() {
    SystemParameter level1 = createParam(SystemParameterKey.SANCTION_LEVEL_1_DAYS, "1", ParameterValueType.INTEGER, 0);
    SystemParameter level2 = createParam(SystemParameterKey.SANCTION_LEVEL_2_DAYS, "15", ParameterValueType.INTEGER, 0);
    SystemParameter level3 = createParam(SystemParameterKey.SANCTION_LEVEL_3_DAYS, "30", ParameterValueType.INTEGER, 0);

    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_2_DAYS))
        .thenReturn(Optional.of(level2));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_1_DAYS))
        .thenReturn(Optional.of(level1));
    when(systemParameterRepository.findByKey(SystemParameterKey.SANCTION_LEVEL_3_DAYS))
        .thenReturn(Optional.of(level3));

    // Cập nhật mức 2 thành 10 (1 < 10 < 30) -> hợp lệ
    UpdateSystemParameterRequest request = new UpdateSystemParameterRequest("10", 0);

    SystemParameterResponse response =
        adminService.updateParameter(SystemParameterKey.SANCTION_LEVEL_2_DAYS, request, 99L);

    assertThat(response.value()).isEqualTo("10");
    verify(systemParameterRepository).saveAndFlush(level2);
  }

  private SystemParameter createParam(
      SystemParameterKey key, String value, ParameterValueType type, int version) {
    SystemParameter param = new SystemParameter();
    param.setKey(key);
    param.setValue(value);
    param.setValueType(type);
    param.setVersion(version);
    return param;
  }
}
