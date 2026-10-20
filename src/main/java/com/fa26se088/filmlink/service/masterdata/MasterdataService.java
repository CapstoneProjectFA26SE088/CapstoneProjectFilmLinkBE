package com.fa26se088.filmlink.service.masterdata;

import com.fa26se088.filmlink.dto.masterdata.response.FilmFormatResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmProcessResponse;
import com.fa26se088.filmlink.dto.masterdata.response.FilmStockResponse;
import com.fa26se088.filmlink.dto.masterdata.response.ServiceTypeResponse;
import java.util.List;

/**
 * Đọc danh mục thông số kỹ thuật ngành phim (MD-01, UC53, BR-16).
 *
 * <p>API công khai (No-Auth) dùng để đổ dữ liệu vào ô chọn (Dropdown/Select), form tạo đơn tráng và bộ
 * lọc tìm kiếm.
 */
public interface MasterdataService {

  /**
   * Lấy danh sách quy trình hóa chất đang hoạt động (ACTIVE).
   *
   * <p>BR-16: Chỉ trả về bản ghi có {@code status = ACTIVE}, không trả {@code DISCONTINUED}.
   */
  List<FilmProcessResponse> getFilmProcesses();

  /**
   * Lấy danh sách khổ phim đang hoạt động (ACTIVE).
   *
   * <p>BR-16: Chỉ trả về bản ghi có {@code status = ACTIVE}.
   */
  List<FilmFormatResponse> getFilmFormats();

  /**
   * Lấy danh sách loại dịch vụ đang hoạt động (ACTIVE).
   *
   * <p>BR-16: Chỉ trả về bản ghi có {@code status = ACTIVE}.
   */
  List<ServiceTypeResponse> getServiceTypes();

  /**
   * Lấy danh sách dòng phim thương mại đang hoạt động (ACTIVE), có hỗ trợ tìm kiếm theo từ khoá.
   *
   * <p>BR-16: Chỉ trả về bản ghi có {@code status = ACTIVE}. Tìm kiếm không phân biệt hoa thường theo
   * tên dòng phim hoặc tên hãng sản xuất.
   *
   * @param query Từ khoá tìm kiếm (có thể null hoặc rỗng để lấy tất cả)
   */
  List<FilmStockResponse> getFilmStocks(String query);
}
