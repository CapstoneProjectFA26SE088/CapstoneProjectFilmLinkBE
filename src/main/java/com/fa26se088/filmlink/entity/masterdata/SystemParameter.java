package com.fa26se088.filmlink.entity.masterdata;

import com.fa26se088.filmlink.entity.BaseEntity;
import com.fa26se088.filmlink.entity.account.UserAccount;
import com.fa26se088.filmlink.entity.masterdata.enums.ParameterValueType;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham số cấu hình toàn hệ thống (ERD #52, UC54, BR-38).
 * Quản lý theo cơ chế Key-Value với kiểu dữ liệu rõ ràng, có Optimistic Locking để tránh ghi đè.
 */
@Getter
@Setter
@Entity
@Table(name = "system_parameters")
public class SystemParameter extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "param_key", nullable = false, unique = true, length = 100)
  private SystemParameterKey key;

  @Column(name = "param_value", nullable = false, length = 255)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false, length = 20)
  private ParameterValueType valueType;

  @Column(name = "description", length = 500)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_account_id")
  private UserAccount updatedBy;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version = 0;
}
