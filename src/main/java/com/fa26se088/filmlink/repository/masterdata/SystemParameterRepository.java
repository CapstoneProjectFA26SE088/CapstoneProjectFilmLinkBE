package com.fa26se088.filmlink.repository.masterdata;

import com.fa26se088.filmlink.entity.masterdata.SystemParameter;
import com.fa26se088.filmlink.entity.masterdata.enums.SystemParameterKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {

  Optional<SystemParameter> findByKey(SystemParameterKey key);

  List<SystemParameter> findAllByOrderByKeyAsc();
}
