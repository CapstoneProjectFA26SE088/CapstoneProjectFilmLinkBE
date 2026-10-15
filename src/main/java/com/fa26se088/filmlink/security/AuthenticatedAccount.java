package com.fa26se088.filmlink.security;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Principal của request đã đăng nhập. Controller lấy bằng {@code @AuthenticationPrincipal
 * AuthenticatedAccount account}.
 *
 * <p>Authority là {@code ROLE_<role>}; quyền kế thừa (BR-05) do {@code RoleHierarchy} xử lý.
 */
public record AuthenticatedAccount(Long id, String email, UserAccountRole role) {

  public List<GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }
}
