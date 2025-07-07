package com.hotelJB.hotelJB_API.controllers;

import com.hotelJB.hotelJB_API.models.entities.OtaIcalConfig;
import com.hotelJB.hotelJB_API.services.OtaIcalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ota-ical")
public class OtaIcalConfigController {

    @Autowired
    private OtaIcalConfigService service;

    @GetMapping
    public List<OtaIcalConfig> getAllActive() {
        return service.getAllActiveConfigs();
    }

    @PostMapping
    public OtaIcalConfig saveConfig(@RequestBody OtaIcalConfig config) {
        return service.save(config);
    }

    @PutMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
