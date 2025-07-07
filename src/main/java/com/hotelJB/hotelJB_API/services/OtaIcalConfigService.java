package com.hotelJB.hotelJB_API.services;

import com.hotelJB.hotelJB_API.models.entities.OtaIcalConfig;
import com.hotelJB.hotelJB_API.repositories.OtaIcalConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OtaIcalConfigService {

    @Autowired
    private OtaIcalConfigRepository repository;

    public List<OtaIcalConfig> getAllActiveConfigs() {
        return repository.findAllByActiveTrue();
    }
}
