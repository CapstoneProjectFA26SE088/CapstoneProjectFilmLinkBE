package com.fa26se088.filmlink.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Trả 401 dạng {@code ProblemDetail} khi chưa đăng nhập hoặc access token không hợp lệ.
 *
 * <p>Lỗi xảy ra trong filter, chưa vào controller, nên chuyển cho {@code HandlerExceptionResolver}
 * của Spring MVC: {@code GlobalExceptionHandler} xử lý như mọi lỗi khác, cùng định dạng {@code
 * errorCode} / {@code messageCode}.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final HandlerExceptionResolver resolver;

  public ProblemDetailAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    resolver.resolveException(request, response, null, authException);
  }
}
