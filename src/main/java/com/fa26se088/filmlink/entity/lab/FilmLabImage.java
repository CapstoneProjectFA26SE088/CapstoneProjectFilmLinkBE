package com.fa26se088.filmlink.entity.lab;

import com.fa26se088.filmlink.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Ảnh mẫu của Film Lab (UC12), bảng {@code film_lab_images}. Tối đa 20 ảnh mỗi lab.
 *
 * <p>Chỉ lưu blobKey, không lưu URL. {@link #displayOrder} nằm trong 1..20 và duy nhất trong một lab
 * ({@code uq_film_lab_images_order}, không hoãn được), nên không đánh lại thứ tự sau khi xoá.
 */
@Getter
@Setter
@Entity
@Table(name = "film_lab_images")
public class FilmLabImage extends BaseEntity {

  @Column(name = "film_lab_id", nullable = false)
  private Long filmLabId;

  @Column(name = "blob_key", nullable = false, length = 500)
  private String blobKey;

  @Column(name = "display_order", nullable = false)
  private Short displayOrder;
}
