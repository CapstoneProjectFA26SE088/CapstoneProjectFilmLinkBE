package com.fa26se088.filmlink.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Trả 403 dạng {@code ProblemDetail} khi bị chặn ở tầng URL. Lỗi 403 từ {@code @PreAuthorize}
 * trong controller đi thẳng vào {@code GlobalExceptionHandler}.
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private final HandlerExceptionResolver resolver;

  public ProblemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) {
    resolver.resolveException(request, response, null, accessDeniedException);
  }
}
