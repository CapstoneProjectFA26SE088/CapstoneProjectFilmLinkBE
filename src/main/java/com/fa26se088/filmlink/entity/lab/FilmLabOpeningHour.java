package com.fa26se088.filmlink.entity.lab;

import com.fa26se088.filmlink.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Giờ mở cửa của Film Lab theo ngày trong tuần (UC12), bảng {@code film_lab_opening_hours}.
 *
 * <p>Mỗi lab tối đa một dòng mỗi ngày ({@code uq_film_lab_opening_hours_day}). Ngày nghỉ: {@link
 * #closed} và không có giờ; ngày mở: có cả hai giờ và {@code opensAt < closesAt} ({@code
 * ck_film_lab_opening_hours_times}).
 */
@Getter
@Setter
@Entity
@Table(name = "film_lab_opening_hours")
public class FilmLabOpeningHour extends BaseEntity {

  @Column(name = "film_lab_id", nullable = false)
  private Long filmLabId;

  /** ISO-8601: 1 = Thứ Hai … 7 = Chủ nhật. */
  @Column(name = "day_of_week", nullable = false)
  private Short dayOfWeek;

  @Column(name = "is_closed", nullable = false)
  private boolean closed = false;

  @Column(name = "opens_at")
  private LocalTime opensAt;

  @Column(name = "closes_at")
  private LocalTime closesAt;
}
