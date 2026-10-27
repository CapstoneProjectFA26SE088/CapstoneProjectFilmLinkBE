package com.fa26se088.filmlink.service.lab;

/**
 * BR-57: lab đã đủ máy để nhận đơn chưa.
 *
 * <p>Bảng {@code machines} thuộc domain {@code machine} (tầng 2), cao hơn {@code lab} (tầng 1), nên
 * {@code lab} không gọi service của {@code machine}. Thay vào đó {@code lab} khai báo interface này
 * (đảo phụ thuộc); domain {@code machine} cung cấp implementation.
 */
public interface MachineReadiness {

  /**
   * BR-57: lab có ít nhất một máy PROCESSOR và ít nhất một máy SCANNER đang ở trạng thái ACTIVE.
   */
  boolean hasActiveProcessorAndScanner(Long filmLabId);
}
