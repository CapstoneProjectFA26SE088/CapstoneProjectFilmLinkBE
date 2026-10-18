package com.fa26se088.filmlink.client.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Loại file storage nhận biết được. Loại file xác định bằng magic bytes đầu file, không tin {@code
 * Content-Type} hay đuôi file do client gửi.
 */
public enum StorageFileType {
  JPEG("jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
  PNG(
      "png",
      "image/png",
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
  PDF("pdf", "application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

  /** Số byte đầu file cần đọc để nhận biết mọi loại ở trên. */
  static final int HEADER_LENGTH = 8;

  private final String extension;
  private final String contentType;
  private final byte[] magic;

  StorageFileType(String extension, String contentType, byte[] magic) {
    this.extension = extension;
    this.contentType = contentType;
    this.magic = magic;
  }

  /** Phần mở rộng (không có dấu chấm) dùng trong blobKey. */
  public String extension() {
    return extension;
  }

  public String contentType() {
    return contentType;
  }

  /** Nhận biết loại file từ các byte đầu; không khớp loại nào thì rỗng. */
  static Optional<StorageFileType> detect(byte[] header) {
    return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
  }

  static Optional<StorageFileType> fromExtension(String extension) {
    return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
  }

  private boolean matches(byte[] header) {
    return header.length >= magic.length
        && Arrays.equals(header, 0, magic.length, magic, 0, magic.length);
  }
}
