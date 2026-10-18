package com.fa26se088.filmlink.client.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint tải file của provider {@code local}, đóng vai link ký của cloud storage (SAS URL).
 *
 * <p>Đặt trong {@code client/storage} thay vì {@code controller/} vì nó là một phần của bản {@link
 * LocalStorageClient}: đổi provider thì endpoint này biến mất cùng. Không cần đăng nhập ({@code
 * permitAll} trong {@code SecurityConfig}): chữ ký chính là quyền truy cập (NFR-PRI-02).
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "filmlink.storage.provider", havingValue = "local")
@Tag(name = "File", description = "Tải file qua link ký (provider local)")
public class LocalFileController {

  private final LocalStorageClient localStorageClient;

  /** Sai chữ ký hoặc hết hạn đều trả 403 {@code STORAGE_LINK_INVALID}. */
  @GetMapping(LocalStorageClient.DOWNLOAD_PATH)
  @Operation(summary = "Tải file qua link ký", description = "Link lấy từ StorageClient.signedUrl.")
  public ResponseEntity<Resource> download(
      @RequestParam(required = false) String key,
      @RequestParam(required = false) String expires,
      @RequestParam(required = false) String signature) {
    LocalStorageClient.LocalStoredFile file = localStorageClient.open(key, expires, signature);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.type().contentType()))
        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(file.secondsLeft())).cachePrivate())
        .header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(file.path()));
  }
}
