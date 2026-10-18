package com.fa26se088.filmlink.client.storage;

import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Bản {@link StorageClient} cho máy dev: file nằm dưới {@code filmlink.storage.local.root-dir}, link
 * tải là {@code GET /api/v1/files?key=&expires=&signature=} ký HMAC-SHA256 (xem {@link
 * LocalFileController}).
 *
 * <p>Chỉ được tạo khi {@code filmlink.storage.provider=local}. Thiếu {@code root-dir} hoặc {@code
 * signing-secret} thì app dừng khi khởi động.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "filmlink.storage.provider", havingValue = "local")
public class LocalStorageClient implements StorageClient {

  static final String DOWNLOAD_PATH = "/api/v1/files";

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final int MIN_SECRET_BYTES = 32;
  private static final DateTimeFormatter KEY_MONTH =
      DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

  /** {@code <purpose>/<yyyy>/<MM>/<uuid>.<ext>}: chặn mọi key không do server sinh. */
  private static final Pattern BLOB_KEY_PATTERN =
      Pattern.compile(
          "("
              + alternatives(Arrays.stream(StoragePurpose.values()).map(StoragePurpose::keyPrefix))
              + ")/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.("
              + alternatives(Arrays.stream(StorageFileType.values()).map(StorageFileType::extension))
              + ")");

  private final Path rootDir;
  private final String publicBaseUrl;
  private final byte[] signingKey;
  private final Duration signedUrlTtl;
  private final Clock clock;

  public LocalStorageClient(StorageProperties properties, Clock clock) {
    StorageProperties.Local local =
        properties.local() == null
            ? new StorageProperties.Local(null, null, null)
            : properties.local();
    this.rootDir =
        createRootDir(requireSetting(local.rootDir(), "root-dir", "STORAGE_LOCAL_ROOT"));
    this.publicBaseUrl =
        stripTrailingSlash(
            requireSetting(local.publicBaseUrl(), "public-base-url", "STORAGE_PUBLIC_BASE_URL"));
    this.signingKey =
        decodeSecret(
            requireSetting(local.signingSecret(), "signing-secret", "STORAGE_SIGNING_SECRET"));
    this.signedUrlTtl = properties.signedUrlTtl();
    this.clock = clock;
    log.info("Storage provider=local, root-dir={}", rootDir);
  }

  @Override
  public String upload(
      StoragePurpose purpose, String originalFilename, InputStream content, long size) {
    if (size > purpose.maxSizeBytes()) {
      throw tooLarge(purpose);
    }
    try {
      BufferedInputStream input = new BufferedInputStream(content);
      input.mark(StorageFileType.HEADER_LENGTH);
      byte[] header = input.readNBytes(StorageFileType.HEADER_LENGTH);
      input.reset();
      StorageFileType type =
          StorageFileType.detect(header)
              .filter(purpose.allowedTypes()::contains)
              .orElseThrow(() -> typeNotAllowed(purpose));

      String blobKey = newBlobKey(purpose, type);
      Path target = resolve(blobKey);
      Files.createDirectories(target.getParent());
      Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
      try {
        long written = copyWithLimit(input, temp, purpose.maxSizeBytes());
        if (written > purpose.maxSizeBytes()) {
          throw tooLarge(purpose);
        }
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temp);
      }
      return blobKey;
    } catch (IOException ex) {
      throw unavailable("upload", ex);
    }
  }

  @Override
  public void delete(String blobKey) {
    Path path = resolveOrThrow(blobKey);
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      throw unavailable("delete", ex);
    }
  }

  @Override
  public URI signedUrl(String blobKey) {
    resolveOrThrow(blobKey);
    long expires = clock.instant().plus(signedUrlTtl).getEpochSecond();
    return UriComponentsBuilder.fromUriString(publicBaseUrl + DOWNLOAD_PATH)
        .queryParam("key", blobKey)
        .queryParam("expires", expires)
        .queryParam("signature", sign(blobKey, expires))
        .encode()
        .build()
        .toUri();
  }

  /**
   * Kiểm link tải rồi trả file. Sai chữ ký, hết hạn, key sai mẫu đều trả chung {@code
   * STORAGE_LINK_INVALID}, không tiết lộ lý do.
   */
  LocalStoredFile open(String blobKey, String expires, String signature) {
    if (blobKey == null || expires == null || signature == null) {
      throw linkInvalid();
    }
    Path path = resolve(blobKey);
    long expiresEpochSeconds;
    try {
      expiresEpochSeconds = Long.parseLong(expires);
    } catch (NumberFormatException ex) {
      throw linkInvalid();
    }
    if (path == null
        || !MessageDigest.isEqual(
            sign(blobKey, expiresEpochSeconds).getBytes(StandardCharsets.US_ASCII),
            signature.getBytes(StandardCharsets.US_ASCII))) {
      throw linkInvalid();
    }
    long secondsLeft = expiresEpochSeconds - clock.instant().getEpochSecond();
    if (secondsLeft <= 0) {
      throw linkInvalid();
    }
    if (!Files.isRegularFile(path)) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "File không còn tồn tại.");
    }
    String extension = blobKey.substring(blobKey.lastIndexOf('.') + 1);
    StorageFileType type = StorageFileType.fromExtension(extension).orElseThrow(this::linkInvalid);
    return new LocalStoredFile(path, type, Math.min(secondsLeft, signedUrlTtl.toSeconds()));
  }

  /** File đã kiểm link, kèm số giây link còn hiệu lực (dùng cho {@code Cache-Control}). */
  record LocalStoredFile(Path path, StorageFileType type, long secondsLeft) {}

  private String newBlobKey(StoragePurpose purpose, StorageFileType type) {
    Instant now = clock.instant();
    return purpose.keyPrefix()
        + "/"
        + KEY_MONTH.format(now)
        + "/"
        + UUID.randomUUID()
        + "."
        + type.extension();
  }

  /**
   * Đường dẫn của blobKey trong root-dir; {@code null} nếu key sai mẫu hoặc thoát ra ngoài root-dir
   * (path traversal).
   */
  private Path resolve(String blobKey) {
    if (blobKey == null || !BLOB_KEY_PATTERN.matcher(blobKey).matches()) {
      return null;
    }
    Path path = rootDir.resolve(blobKey).normalize();
    return path.startsWith(rootDir) ? path : null;
  }

  /** blobKey lấy từ DB mà sai mẫu là lỗi lập trình, không phải lỗi người dùng. */
  private Path resolveOrThrow(String blobKey) {
    Path path = resolve(blobKey);
    if (path == null) {
      throw new IllegalArgumentException("blobKey không đúng mẫu <purpose>/<yyyy>/<MM>/<uuid>.<ext>");
    }
    return path;
  }

  /** Chép tối đa {@code limit + 1} byte: vượt {@code limit} nghĩa là file quá lớn. */
  private static long copyWithLimit(InputStream input, Path target, long limit) throws IOException {
    try (OutputStream output = Files.newOutputStream(target)) {
      byte[] buffer = new byte[8192];
      long total = 0;
      int read;
      while (total <= limit && (read = input.read(buffer)) != -1) {
        output.write(buffer, 0, read);
        total += read;
      }
      return total;
    }
  }

  private String sign(String blobKey, long expiresEpochSeconds) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(signingKey, HMAC_SHA256));
      byte[] digest =
          mac.doFinal((blobKey + "\n" + expiresEpochSeconds).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Không ký được link storage", ex);
    }
  }

  /**
   * Biến môi trường không có (placeholder {@code ${...}} không giải được) hoặc rỗng thì dừng app với
   * thông báo rõ.
   */
  private static String requireSetting(String value, String property, String envName) {
    if (value == null || value.isBlank() || value.contains("${")) {
      throw new IllegalStateException(
          "Thiếu filmlink.storage.local."
              + property
              + " (biến "
              + envName
              + "): bắt buộc khi filmlink.storage.provider=local");
    }
    return value.trim();
  }

  private static Path createRootDir(String rootDir) {
    Path path = Path.of(rootDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new IllegalStateException(
          "Không tạo được thư mục filmlink.storage.local.root-dir (STORAGE_LOCAL_ROOT)", ex);
    }
    return path;
  }

  private static byte[] decodeSecret(String secret) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("STORAGE_SIGNING_SECRET phải là chuỗi Base64", ex);
    }
    if (key.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("STORAGE_SIGNING_SECRET phải dài tối thiểu 256 bit");
    }
    return key;
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static String alternatives(Stream<String> values) {
    return values.map(Pattern::quote).collect(Collectors.joining("|"));
  }

  private static BusinessException typeNotAllowed(StoragePurpose purpose) {
    String allowed =
        purpose.allowedTypes().stream()
            .map(type -> type.extension().toUpperCase(Locale.ROOT))
            .sorted()
            .collect(Collectors.joining(", "));
    return new BusinessException(
        ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED, "Loại file không được hỗ trợ. Chỉ nhận: " + allowed);
  }

  private static BusinessException tooLarge(StoragePurpose purpose) {
    return new BusinessException(
        ErrorCode.STORAGE_FILE_TOO_LARGE,
        "File vượt quá dung lượng cho phép (" + purpose.maxSizeBytes() / (1024 * 1024) + " MB).");
  }

  private BusinessException linkInvalid() {
    return new BusinessException(
        ErrorCode.STORAGE_LINK_INVALID, "Link tải file không hợp lệ hoặc đã hết hạn.");
  }

  /** UH-39: ghi log lỗi kỹ thuật, trả 503 với thông báo chung; phần khác của app vẫn chạy. */
  private static BusinessException unavailable(String operation, IOException ex) {
    log.error("Storage {} failed", operation, ex);
    return new BusinessException(
        ErrorCode.STORAGE_UNAVAILABLE, "Dịch vụ lưu trữ file tạm thời không khả dụng.");
  }
}
