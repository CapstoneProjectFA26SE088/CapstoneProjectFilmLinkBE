package com.fa26se088.filmlink.repository.masterdata;

import com.fa26se088.filmlink.entity.masterdata.ServiceType;
import com.fa26se088.filmlink.entity.masterdata.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

  List<ServiceType> findByStatusOrderByDisplayNameAsc(CatalogStatus status);

  List<ServiceType> findAllByOrderByDisplayNameAsc();

  Optional<ServiceType> findByCode(String code);

  boolean existsByCode(String code);
}
