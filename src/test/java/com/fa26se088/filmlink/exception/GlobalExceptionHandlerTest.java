package com.fa26se088.filmlink.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  /** Lỗi 403 từ {@code @PreAuthorize} trả 403, không còn bị biến thành 500. */
  @Test
  void accessDeniedBecomes403() {
    ProblemDetail problem = handler.handleAccessDeniedException(new AccessDeniedException("x"));

    assertThat(problem.getStatus()).isEqualTo(403);
    assertThat(problem.getProperties()).containsEntry("errorCode", "ACCESS_DENIED");
  }

  @Test
  void businessExceptionCarriesErrorCodeMessageCodeAndProperties() {
    ProblemDetail problem =
        handler.handleBusinessException(
            new BusinessException(
                ErrorCode.ACCOUNT_INVALID_CREDENTIALS, "x", Map.of("extra", "value")));

    assertThat(problem.getStatus()).isEqualTo(401);
    assertThat(problem.getProperties())
        .containsEntry("errorCode", "ACCOUNT_INVALID_CREDENTIALS")
        .containsEntry("messageCode", "MSG09")
        .containsEntry("extra", "value");
  }

  /** Multipart vượt {@code spring.servlet.multipart.*}: 413 với mã của storage. */
  @Test
  void maxUploadSizeExceededBecomesStorageFileTooLarge() throws Exception {
    ResponseEntity<Object> response =
        handler.handleException(
            new MaxUploadSizeExceededException(10L * 1024 * 1024),
            new ServletWebRequest(new MockHttpServletRequest()));

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().value()).isEqualTo(413);
    assertThat(((ProblemDetail) response.getBody()).getProperties())
        .containsEntry("errorCode", "STORAGE_FILE_TOO_LARGE");
  }

  /** CR-06: lỗi không lường trước không lộ thông điệp kỹ thuật. */
  @Test
  void unexpectedExceptionHidesDetails() {
    ProblemDetail problem =
        handler.handleUnexpectedException(new IllegalStateException("relation user_accounts"));

    assertThat(problem.getStatus()).isEqualTo(500);
    assertThat(problem.getDetail()).doesNotContain("user_accounts");
  }
}
