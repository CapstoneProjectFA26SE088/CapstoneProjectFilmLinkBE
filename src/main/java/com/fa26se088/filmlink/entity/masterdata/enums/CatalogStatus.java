package com.fa26se088.filmlink.entity.masterdata.enums;

/**
 * Trạng thái của các danh mục master data (BR-16).
 *
 * <ul>
 *   <li>{@code ACTIVE}: đang áp dụng, được hiển thị cho khách hàng và lab khi tạo đơn/bảng giá.
 *   <li>{@code DISCONTINUED}: ngừng hỗ trợ/ngừng sản xuất, không được hiển thị khi tạo mới.
 * </ul>
 */
public enum CatalogStatus {
  ACTIVE,
  DISCONTINUED
}
