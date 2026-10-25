package com.fa26se088.filmlink.mapper.lab;

import com.fa26se088.filmlink.dto.lab.response.FilmLabImageResponse;
import com.fa26se088.filmlink.dto.lab.response.FilmLabOpeningHourResponse;
import com.fa26se088.filmlink.dto.lab.response.FilmLabProfileResponse;
import com.fa26se088.filmlink.entity.lab.FilmLab;
import com.fa26se088.filmlink.entity.lab.FilmLabImage;
import com.fa26se088.filmlink.entity.lab.FilmLabOpeningHour;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chuyển entity của domain lab sang DTO. Viết tay, không dùng MapStruct. Link ảnh do service tạo
 * (qua {@code StorageClient}) rồi truyền vào, mapper không gọi client.
 */
@Component
public class FilmLabMapper {

  public FilmLabProfileResponse toProfileResponse(
      FilmLab lab,
      List<FilmLabOpeningHour> openingHours,
      List<FilmLabImageResponse> images) {
    return new FilmLabProfileResponse(
        lab.getId(),
        lab.getLabName(),
        lab.getDescription(),
        lab.getAddressLine(),
        lab.getDistrict(),
        lab.getProvince(),
        lab.getLatitude(),
        lab.getLongitude(),
        lab.getContactEmail(),
        lab.getContactPhone(),
        lab.isAcceptingOrders(),
        lab.getQcBufferHours(),
        lab.getExpressDailyCapPercent(),
        openingHours.stream().map(this::toOpeningHourResponse).toList(),
        images,
        lab.getCreatedAt(),
        lab.getUpdatedAt());
  }

  public FilmLabOpeningHourResponse toOpeningHourResponse(FilmLabOpeningHour hour) {
    return new FilmLabOpeningHourResponse(
        hour.getDayOfWeek(), hour.isClosed(), hour.getOpensAt(), hour.getClosesAt());
  }

  public FilmLabImageResponse toImageResponse(FilmLabImage image, URI url) {
    return new FilmLabImageResponse(image.getId(), image.getDisplayOrder(), url);
  }
}
