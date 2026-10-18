package com.fa26se088.filmlink.client.storage;

import java.io.InputStream;
import java.net.URI;

/**
 * Lưu trữ file (ảnh, giấy tờ, scan). Bản dùng được chọn bằng property {@code
 * filmlink.storage.provider}:
 *
 * <ul>
 *   <li>{@code local}: {@link LocalStorageClient}, lưu trong thư mục trên máy dev.
 *   <li>Azure Blob Storage: chưa có, thêm một class mới khi triển khai.
 * </ul>
 *
 * <p>DB chỉ lưu blobKey (cột {@code blob_key}, {@code avatar_blob_key}), không bao giờ lưu đường dẫn
 * tuyệt đối hay URL. blobKey do server sinh theo mẫu {@code <purpose>/<yyyy>/<MM>/<uuid>.<ext>}.
 *
 * <p>Lỗi trả về dạng {@code BusinessException}: {@code STORAGE_FILE_TYPE_NOT_ALLOWED}, {@code
 * STORAGE_FILE_TOO_LARGE}, {@code STORAGE_UNAVAILABLE} (UH-39: storage lỗi thì báo rõ, phần khác
 * vẫn chạy).
 */
public interface StorageClient {

  /**
   * Kiểm tra loại file (magic bytes) và dung lượng theo {@code purpose}, rồi lưu file.
   *
   * @param purpose mục đích, quyết định loại file và dung lượng cho phép
   * @param originalFilename tên file người dùng gửi; không dùng trong blobKey, không dùng để kiểm
   *     loại file
   * @param content nội dung file; người gọi tự đóng stream
   * @param size dung lượng khai báo (byte), ví dụ {@code MultipartFile.getSize()}
   * @return blobKey để lưu vào DB
   */
  String upload(StoragePurpose purpose, String originalFilename, InputStream content, long size);

  /** Xoá file. Không lỗi nếu file đã không còn. */
  void delete(String blobKey);

  /** Link tải có chữ ký, hết hạn sau {@code filmlink.storage.signed-url-ttl} (NFR-PRI-02). */
  URI signedUrl(String blobKey);
}
