package com.fa26se088.filmlink.entity.account.enums;

/**
 * Role của tài khoản, lưu ở cột {@code user_accounts.role}.
 *
 * <p>BR-01: mỗi tài khoản đúng một role. BR-05: ADMIN có mọi quyền của MODERATOR;
 * PHOTOGRAPHY_EXPERT có mọi quyền của PHOTOGRAPHER (cấu hình ở {@code RoleHierarchy} trong
 * {@code SecurityConfig}).
 */
public enum UserAccountRole {
  PHOTOGRAPHER,
  PHOTOGRAPHY_EXPERT,
  FILM_LAB_OWNER,
  MODERATOR,
  ADMIN
}
