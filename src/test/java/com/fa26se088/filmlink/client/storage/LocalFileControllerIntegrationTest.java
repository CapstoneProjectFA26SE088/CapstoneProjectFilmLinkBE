package com.fa26se088.filmlink.client.storage;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Link tải {@code GET /api/v1/files} không cần đăng nhập (chữ ký là quyền truy cập), sai chữ ký hay
 * hết hạn đều 403. {@link Clock} được thay bằng mock để test hết hạn mà không sleep.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalFileControllerIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-10-18T03:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private StorageClient storageClient;

  @MockitoBean private Clock clock;

  private final byte[] png = LocalStorageClientTest.png(256);
  private String blobKey;

  @BeforeEach
  void upload() {
    when(clock.instant()).thenReturn(NOW);
    blobKey =
        storageClient.upload(
            StoragePurpose.AVATAR, "avatar.png", new ByteArrayInputStream(png), png.length);
  }

  @AfterEach
  void cleanUp() {
    storageClient.delete(blobKey);
  }

  @Test
  void validLinkDownloadsWithoutLogin() throws Exception {
    Map<String, String> params = signedParams();
    when(clock.instant()).thenReturn(NOW.plusSeconds(300));

    mockMvc
        .perform(download(params))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Cache-Control", "max-age=600, private"))
        .andExpect(content().bytes(png));
  }

  @Test
  void tamperedSignatureReturns403() throws Exception {
    Map<String, String> params = signedParams();
    String signature = params.get("signature");
    params.put("signature", signature.substring(0, signature.length() - 1) + (signature.endsWith("A") ? "B" : "A"));

    mockMvc
        .perform(download(params))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_LINK_INVALID"));
  }

  @Test
  void expiredLinkReturns403() throws Exception {
    Map<String, String> params = signedParams();
    when(clock.instant()).thenReturn(NOW.plusSeconds(15 * 60));

    mockMvc
        .perform(download(params))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_LINK_INVALID"));
  }

  @Test
  void missingParametersReturn403() throws Exception {
    mockMvc
        .perform(get("/api/v1/files").param("key", blobKey))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_LINK_INVALID"));
  }

  private Map<String, String> signedParams() {
    return LocalStorageClientTest.queryParams(storageClient.signedUrl(blobKey));
  }

  private static MockHttpServletRequestBuilder download(Map<String, String> params) {
    return get("/api/v1/files")
        .param("key", params.get("key"))
        .param("expires", params.get("expires"))
        .param("signature", params.get("signature"));
  }
}
