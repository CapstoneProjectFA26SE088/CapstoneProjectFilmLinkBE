package com.fa26se088.filmlink.service.masterdata;

import com.fa26se088.filmlink.dto.masterdata.request.UpdateSystemParameterRequest;
import com.fa26se088.filmlink.dto.masterdata.response.SystemParameterResponse;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import java.util.List;

/**
 * Service quản trị tham số hệ thống dành riêng cho Admin (UC54, BR-38).
 * Xem danh sách, cập nhật có kiểm tra miền giá trị, so sánh before/after, optimistic lock và ghi audit.
 */
public interface SystemParameterAdminService {

  /** Lấy toàn bộ danh sách tham số hệ thống. */
  List<SystemParameterResponse> getAllParameters();

  /** Lấy thông tin chi tiết một tham số theo key. */
  SystemParameterResponse getParameter(SystemParameterKey key);

  /** Cập nhật giá trị tham số hệ thống (kiểm tra miền giá trị, optimistic lock, audit log). */
  SystemParameterResponse updateParameter(
      SystemParameterKey key, UpdateSystemParameterRequest request, Long adminAccountId);
}
