package com.fa26se088.filmlink.client.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình lưu trữ file, prefix {@code filmlink.storage} trong {@code application.yml}.
 *
 * @param provider bản {@link StorageClient} được dùng ({@code STORAGE_PROVIDER}); hiện chỉ có
 *     {@code local}
 * @param signedUrlTtl thời hạn link ký ({@code STORAGE_SIGNED_URL_TTL}), tối đa 15 phút
 *     (NFR-PRI-02)
 * @param local cấu hình của {@link LocalStorageClient}, chỉ bắt buộc khi {@code provider = local}
 */
@Validated
@ConfigurationProperties(prefix = "filmlink.storage")
public record StorageProperties(
    @NotBlank String provider, @NotNull Duration signedUrlTtl, @Valid Local local) {

  /** NFR-PRI-02: link truy cập file riêng tư hết hạn tối đa sau 15 phút. */
  public static final Duration MAX_SIGNED_URL_TTL = Duration.ofMinutes(15);

  @AssertTrue(message = "filmlink.storage.signed-url-ttl phải lớn hơn 0 và không quá 15 phút (NFR-PRI-02)")
  public boolean isSignedUrlTtlWithinLimit() {
    return signedUrlTtl == null
        || (signedUrlTtl.isPositive() && signedUrlTtl.compareTo(MAX_SIGNED_URL_TTL) <= 0);
  }

  /**
   * Cấu hình provider {@code local}: file nằm trong một thư mục trên máy dev.
   *
   * @param rootDir thư mục gốc chứa file ({@code STORAGE_LOCAL_ROOT}), không có giá trị mặc định
   * @param publicBaseUrl gốc URL của backend dùng để dựng link tải ({@code STORAGE_PUBLIC_BASE_URL})
   * @param signingSecret khoá HMAC-SHA256 dạng Base64, tối thiểu 256 bit ({@code
   *     STORAGE_SIGNING_SECRET}), riêng với {@code JWT_SECRET}
   */
  public record Local(String rootDir, String publicBaseUrl, String signingSecret) {}
}
