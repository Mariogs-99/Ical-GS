package com.hotelJB.hotelJB_API.repositories;

import com.hotelJB.hotelJB_API.models.entities.OtaIcalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtaIcalConfigRepository extends JpaRepository<OtaIcalConfig, Long> {

    List<OtaIcalConfig> findAllByActiveTrue();
}
