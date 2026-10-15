package com.fa26se088.filmlink.entity.account;

import com.fa26se088.filmlink.entity.BaseEntity;
import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.entity.account.enums.UserAccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * User Account (ERD #1), bảng {@code user_accounts}.
 *
 * <p>Photographer, Photography Expert, Film Lab Owner, Moderator, Admin là giá trị của {@link
 * #role}, không phải bảng riêng (BR-01). Email luôn lưu chữ thường (CHECK {@code
 * ck_user_accounts_email_lower}). Giá trị khởi tạo của các field khớp {@code DEFAULT} trong DB, vì
 * Hibernate luôn ghi mọi cột khi insert.
 */
@Getter
@Setter
@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Column(name = "phone_number", length = 15)
  private String phoneNumber;

  @Column(name = "avatar_blob_key", length = 500)
  private String avatarBlobKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private UserAccountRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private UserAccountStatus status = UserAccountStatus.PENDING_VERIFICATION;

  @Column(name = "lock_reason", length = 500)
  private String lockReason;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "locked_by_account_id")
  private UserAccount lockedBy;

  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;

  @Column(name = "terms_accepted_at")
  private Instant termsAcceptedAt;

  @Column(name = "failed_login_count", nullable = false)
  private Short failedLoginCount = 0;

  @Column(name = "login_locked_until")
  private Instant loginLockedUntil;

  @Column(name = "is_in_app_notification_enabled", nullable = false)
  private boolean inAppNotificationEnabled = true;

  @Column(name = "is_email_notification_enabled", nullable = false)
  private boolean emailNotificationEnabled = true;

  @Column(name = "is_push_notification_enabled", nullable = false)
  private boolean pushNotificationEnabled = true;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
