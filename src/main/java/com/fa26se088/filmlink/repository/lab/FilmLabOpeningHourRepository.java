package com.fa26se088.filmlink.repository.lab;

import com.fa26se088.filmlink.entity.lab.FilmLabOpeningHour;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilmLabOpeningHourRepository extends JpaRepository<FilmLabOpeningHour, Long> {

  List<FilmLabOpeningHour> findByFilmLabIdOrderByDayOfWeek(Long filmLabId);
}
