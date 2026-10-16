package com.fa26se088.filmlink.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.entity.account.enums.UserAccountRole;
import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentAccountTest {

  private static final AuthenticatedAccount ACCOUNT =
      new AuthenticatedAccount(7L, "owner@example.com", UserAccountRole.FILM_LAB_OWNER);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void withPrincipal_findAndRequireReturnAccount() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                ACCOUNT, null, ACCOUNT.authorities()));

    assertThat(CurrentAccount.find()).contains(ACCOUNT);
    assertThat(CurrentAccount.require()).isEqualTo(ACCOUNT);
  }

  @Test
  void withoutAuthentication_findIsEmptyAndRequireThrows401() {
    assertThat(CurrentAccount.find()).isEmpty();
    assertAuthenticationRequired();
  }

  @Test
  void anonymous_findIsEmptyAndRequireThrows401() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(CurrentAccount.find()).isEmpty();
    assertAuthenticationRequired();
  }

  private static void assertAuthenticationRequired() {
    assertThatThrownBy(CurrentAccount::require)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
  }
}
