package com.fa26se088.filmlink.security;

import com.fa26se088.filmlink.service.account.AuthenticationService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Đọc access token từ header {@code Authorization: Bearer <token>} và đặt {@link
 * AuthenticatedAccount} vào {@code SecurityContext}.
 *
 * <p>Tài khoản được nạp lại từ DB ở mỗi request, chỉ chấp nhận trạng thái ACTIVE: tài khoản bị khoá
 * mất quyền ngay, không phải chờ access token hết hạn.
 *
 * <p>Token sai không bị nuốt im lặng: ghi log mức debug rồi cho request đi tiếp mà không xác thực,
 * để entry point trả 401. Lớp này <b>không</b> là {@code @Component}: chỉ {@link SecurityConfig}
 * tạo và gắn vào chuỗi filter của Spring Security, tránh Spring Boot đăng ký nó thêm một lần làm
 * servlet filter.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationService authenticationService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null
        && header.startsWith(BEARER_PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(String token, HttpServletRequest request) {
    Long accountId;
    try {
      accountId = jwtTokenProvider.parseAccountId(token);
    } catch (JwtException | IllegalArgumentException ex) {
      log.debug("Access token bị từ chối ({}): {}", request.getRequestURI(), ex.getMessage());
      return;
    }
    authenticationService
        .loadActiveAccount(accountId)
        .ifPresentOrElse(
            principal -> setAuthentication(principal, request),
            () -> log.debug("Tài khoản {} không tồn tại hoặc không ACTIVE", accountId));
  }

  private static void setAuthentication(
      AuthenticatedAccount principal, HttpServletRequest request) {
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.authorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
