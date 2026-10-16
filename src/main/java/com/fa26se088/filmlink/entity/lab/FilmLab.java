package com.fa26se088.filmlink.entity.lab;

import com.fa26se088.filmlink.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Film Lab (ERD #8), bảng {@code film_labs}.
 *
 * <p>Chỉ được tạo khi đơn đăng ký lab được duyệt (BR-03). Mỗi Film Lab Owner quản lý đúng một lab
 * ({@code uq_film_labs_owner}); trigger {@code film_labs_owner_role} bắt owner phải có role
 * FILM_LAB_OWNER. Khoá ngoại giữ dạng {@code Long} vì chưa có entity Lab Registration Application.
 * Giá trị khởi tạo khớp {@code DEFAULT} trong DB.
 */
@Getter
@Setter
@Entity
@Table(name = "film_labs")
public class FilmLab extends BaseEntity {

  @Column(name = "owner_account_id", nullable = false)
  private Long ownerAccountId;

  @Column(name = "lab_registration_application_id", nullable = false)
  private Long labRegistrationApplicationId;

  @Column(name = "lab_name", nullable = false, length = 150)
  private String labName;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "address_line", nullable = false, length = 255)
  private String addressLine;

  @Column(name = "district", nullable = false, length = 100)
  private String district;

  @Column(name = "province", nullable = false, length = 100)
  private String province;

  @Column(name = "latitude", precision = 9, scale = 6)
  private BigDecimal latitude;

  @Column(name = "longitude", precision = 9, scale = 6)
  private BigDecimal longitude;

  @Column(name = "contact_email", nullable = false, length = 255)
  private String contactEmail;

  @Column(name = "contact_phone", nullable = false, length = 15)
  private String contactPhone;

  /** UC12: lab mới chưa nhận đơn, không hiện trong tìm kiếm cho tới khi chủ lab mở (BR-34). */
  @Column(name = "is_accepting_orders", nullable = false)
  private boolean acceptingOrders = false;

  /** BR-60 / UC68: thời gian đệm QC (giờ làm việc) trong công thức ngày hẹn trả. */
  @Column(name = "qc_buffer_hours", nullable = false)
  private Short qcBufferHours = 4;

  /** BR-61: tỉ lệ tối đa đơn Express mỗi ngày; {@code null} = không giới hạn. */
  @Column(name = "express_daily_cap_percent")
  private Short expressDailyCapPercent;
}
