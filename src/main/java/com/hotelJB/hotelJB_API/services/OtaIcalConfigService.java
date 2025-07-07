package com.hotelJB.hotelJB_API.services;

import com.hotelJB.hotelJB_API.models.entities.OtaIcalConfig;
import com.hotelJB.hotelJB_API.repositories.OtaIcalConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OtaIcalConfigService {

    @Autowired
    private OtaIcalConfigRepository repository;

    /**
     * Obtener todas las integraciones activas
     */
    public List<OtaIcalConfig> getAllActiveConfigs() {
        return repository.findAllByActiveTrue();
    }

    /**
     * Guardar o actualizar una integración OTA
     */
    public OtaIcalConfig save(OtaIcalConfig config) {
        return repository.save(config);
    }

    /**
     * Buscar integración OTA por ID
     */
    public Optional<OtaIcalConfig> getById(Long id) {
        return repository.findById(id);
    }

    /**
     * Desactivar una integración OTA (soft delete)
     */
    public void deactivate(Long id) {
        Optional<OtaIcalConfig> optional = repository.findById(id);
        if (optional.isPresent()) {
            OtaIcalConfig config = optional.get();
            config.setActive(false);
            repository.save(config);
        } else {
            throw new RuntimeException("No se encontró la integración OTA con ID: " + id);
        }
    }

    /**
     * Eliminar físicamente una integración OTA
     */
    public void delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new RuntimeException("No se encontró la integración OTA con ID: " + id);
        }
    }
}
