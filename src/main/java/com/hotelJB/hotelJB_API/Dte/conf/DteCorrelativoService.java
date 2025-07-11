package com.hotelJB.hotelJB_API.Dte.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class DteCorrelativoService {

    @Autowired
    private DteCorrelativoRepository repository;

    @Transactional
    public synchronized long nextCorrelativo(String tipoDte, String codEstable, String codPuntoVenta) {
        DteCorrelativoId id = new DteCorrelativoId(tipoDte, codEstable, codPuntoVenta);
        DteCorrelativo correlativo = repository.findById(id).orElseGet(() -> {
            DteCorrelativo nuevo = new DteCorrelativo();
            nuevo.setTipoDte(tipoDte);
            nuevo.setCodEstable(codEstable);
            nuevo.setCodPuntoVenta(codPuntoVenta);
            nuevo.setCorrelativo(0L);
            return nuevo;
        });

        long nuevoValor = correlativo.getCorrelativo() + 1;
        correlativo.setCorrelativo(nuevoValor);
        repository.save(correlativo);
        return nuevoValor;
    }
}

