package com.fa26se088.filmlink.client.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fa26se088.filmlink.exception.BusinessException;
import com.fa26se088.filmlink.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class LocalStorageClientTest {

  /** Khoá giả sinh trong test (32 byte 0x01), không phải bí mật thật. */
  static final String TEST_SECRET = Base64.getEncoder().encodeToString(filled(32, (byte) 1));

  private static final Instant NOW = Instant.parse("2026-10-18T03:00:00Z");
  private static final Duration TTL = Duration.ofMinutes(15);

  @TempDir Path tempDir;

  private MutableClock clock;
  private LocalStorageClient client;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(NOW);
    client = newClient(tempDir.resolve("root"));
  }

  @Test
  void uploadsJpgPngAndPdfWithServerGeneratedKey() throws Exception {
    String jpgKey = upload(StoragePurpose.AVATAR, "me.jpg", jpeg(100));
    String pngKey = upload(StoragePurpose.LAB_IMAGE, "lab.png", png(100));
    String pdfKey = upload(StoragePurpose.LAB_DOCUMENT, "giay-phep.pdf", pdf(100));

    assertThat(jpgKey).matches("avatar/2026/10/[0-9a-f-]{36}\\.jpg");
    assertThat(pngKey).matches("lab-image/2026/10/[0-9a-f-]{36}\\.png");
    assertThat(pdfKey).matches("lab-document/2026/10/[0-9a-f-]{36}\\.pdf").doesNotContain("giay-phep");
    assertThat(Files.readAllBytes(tempDir.resolve("root").resolve(pdfKey))).isEqualTo(pdf(100));
  }

  /** Đuôi file và tên do người dùng đặt không quyết định loại file: kiểm bằng magic bytes. */
  @Test
  void exeRenamedToPngIsRejected() {
    byte[] exe = withHeader(new byte[] {'M', 'Z', (byte) 0x90, 0}, 100);

    assertError(() -> upload(StoragePurpose.AVATAR, "virus.png", exe), ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED);
  }

  @Test
  void pdfIsNotAllowedAsAvatar() {
    assertError(
        () -> upload(StoragePurpose.AVATAR, "a.pdf", pdf(100)), ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED);
  }

  /** SCR-05: avatar tối đa 5 MB. */
  @Test
  void declaredSizeOverLimitIsRejected() {
    long tooBig = StoragePurpose.AVATAR.maxSizeBytes() + 1;

    assertThatThrownBy(
            () ->
                client.upload(
                    StoragePurpose.AVATAR, "a.png", new ByteArrayInputStream(png(10)), tooBig))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORAGE_FILE_TOO_LARGE);
  }

  /** Khai báo size nhỏ nhưng nội dung thật lớn hơn giới hạn: vẫn bị chặn, không để lại file. */
  @Test
  void actualContentOverLimitIsRejectedAndNothingIsKept() throws Exception {
    byte[] content = png((int) StoragePurpose.AVATAR.maxSizeBytes() + 1);

    assertThatThrownBy(
            () -> client.upload(StoragePurpose.AVATAR, "a.png", new ByteArrayInputStream(content), 10))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORAGE_FILE_TOO_LARGE);
    try (var files = Files.walk(tempDir.resolve("root"))) {
      assertThat(files.filter(Files::isRegularFile)).isEmpty();
    }
  }

  @Test
  void fileAtExactLimitIsAccepted() throws Exception {
    String key = upload(StoragePurpose.AVATAR, "a.png", png((int) StoragePurpose.AVATAR.maxSizeBytes()));

    assertThat(Files.size(tempDir.resolve("root").resolve(key)))
        .isEqualTo(StoragePurpose.AVATAR.maxSizeBytes());
  }

  @Test
  void signedUrlOpensFileUntilExpiry() throws Exception {
    String key = upload(StoragePurpose.AVATAR, "a.png", png(100));
    Map<String, String> params = queryParams(client.signedUrl(key));

    assertThat(params).containsEntry("key", key);
    assertThat(Long.parseLong(params.get("expires"))).isEqualTo(NOW.plus(TTL).getEpochSecond());

    clock.advance(TTL.minusSeconds(60));
    LocalStorageClient.LocalStoredFile file = open(params);
    assertThat(file.type()).isEqualTo(StorageFileType.PNG);
    assertThat(file.secondsLeft()).isEqualTo(60);
  }

  /** NFR-PRI-02: link hết hạn thì 403, cùng mã với sai chữ ký. */
  @Test
  void expiredLinkIsRejected() throws Exception {
    String key = upload(StoragePurpose.AVATAR, "a.png", png(100));
    Map<String, String> params = queryParams(client.signedUrl(key));

    clock.advance(TTL);

    assertError(() -> open(params), ErrorCode.STORAGE_LINK_INVALID);
  }

  @Test
  void tamperedSignatureIsRejected() throws Exception {
    String key = upload(StoragePurpose.AVATAR, "a.png", png(100));
    Map<String, String> params = queryParams(client.signedUrl(key));
    String signature = params.get("signature");
    char first = signature.charAt(0);
    params.put("signature", (first == 'A' ? 'B' : 'A') + signature.substring(1));

    assertError(() -> open(params), ErrorCode.STORAGE_LINK_INVALID);
  }

  /** Kéo dài {@code expires} làm chữ ký không còn khớp. */
  @Test
  void extendedExpiryIsRejected() throws Exception {
    String key = upload(StoragePurpose.AVATAR, "a.png", png(100));
    Map<String, String> params = queryParams(client.signedUrl(key));
    params.put("expires", String.valueOf(Long.parseLong(params.get("expires")) + 3600));

    assertError(() -> open(params), ErrorCode.STORAGE_LINK_INVALID);
  }

  @Test
  void pathTraversalIsRejected() throws Exception {
    Files.writeString(tempDir.resolve("secret.txt"), "x");
    String traversal = "avatar/../../secret.txt";

    assertError(
        () -> client.open(traversal, String.valueOf(NOW.getEpochSecond() + 60), "x"),
        ErrorCode.STORAGE_LINK_INVALID);
    assertThatThrownBy(() -> client.signedUrl(traversal)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> client.delete(traversal)).isInstanceOf(IllegalArgumentException.class);
    assertThat(tempDir.resolve("secret.txt")).exists();
  }

  @Test
  void deleteRemovesFile() throws Exception {
    String key = upload(StoragePurpose.LAB_DOCUMENT, "a.pdf", pdf(100));
    Path stored = tempDir.resolve("root").resolve(key);
    assertThat(stored).exists();

    client.delete(key);

    assertThat(stored).doesNotExist();
    client.delete(key);
  }

  @Test
  void createsRootDirWhenMissing() {
    Path root = tempDir.resolve("a").resolve("b");

    newClient(root);

    assertThat(root).isDirectory();
  }

  /** Placeholder {@code ${STORAGE_LOCAL_ROOT}} không giải được: app dừng với thông báo rõ. */
  @Test
  void missingRootDirStopsStartupWithClearMessage() {
    new ApplicationContextRunner()
        .withUserConfiguration(StorageTestConfig.class)
        .withPropertyValues(
            "filmlink.storage.provider=local",
            "filmlink.storage.signed-url-ttl=PT15M",
            "filmlink.storage.local.root-dir=${STORAGE_LOCAL_ROOT_NOT_SET_IN_TEST}",
            "filmlink.storage.local.public-base-url=http://localhost:8080",
            "filmlink.storage.local.signing-secret=" + TEST_SECRET)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("STORAGE_LOCAL_ROOT"));
  }

  @Test
  void shortSigningSecretStopsStartup() {
    StorageProperties properties =
        properties(
            tempDir.resolve("root"), Base64.getEncoder().encodeToString(filled(16, (byte) 1)));

    assertThatThrownBy(() -> new LocalStorageClient(properties, clock))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("256 bit");
  }

  @Configuration
  @EnableConfigurationProperties(StorageProperties.class)
  @Import(LocalStorageClient.class)
  static class StorageTestConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  private LocalStorageClient newClient(Path root) {
    return new LocalStorageClient(properties(root, TEST_SECRET), clock);
  }

  private static StorageProperties properties(Path root, String secret) {
    return new StorageProperties(
        "local",
        TTL,
        new StorageProperties.Local(root.toString(), "http://localhost:8080/", secret));
  }

  private String upload(StoragePurpose purpose, String filename, byte[] content) {
    return client.upload(purpose, filename, new ByteArrayInputStream(content), content.length);
  }

  private LocalStorageClient.LocalStoredFile open(Map<String, String> params) {
    return client.open(params.get("key"), params.get("expires"), params.get("signature"));
  }

  private static void assertError(
      ThrowingCallable call, ErrorCode errorCode) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(errorCode);
  }

  static Map<String, String> queryParams(URI uri) {
    return Arrays.stream(uri.getRawQuery().split("&"))
        .map(pair -> pair.split("=", 2))
        .collect(
            Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8),
                (a, b) -> b,
                HashMap::new));
  }

  static byte[] jpeg(int size) {
    return withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, size);
  }

  static byte[] png(int size) {
    return withHeader(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, size);
  }

  static byte[] pdf(int size) {
    return withHeader("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), size);
  }

  private static byte[] withHeader(byte[] header, int size) {
    byte[] content = filled(size, (byte) 0x20);
    System.arraycopy(header, 0, content, 0, header.length);
    return content;
  }

  private static byte[] filled(int size, byte value) {
    byte[] bytes = new byte[size];
    Arrays.fill(bytes, value);
    return bytes;
  }

  /** {@link Clock} chỉnh được thời gian, để test hết hạn mà không sleep. */
  static final class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
