package com.hotelJB.hotelJB_API.Dte.conf;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DteCorrelativoRepository extends JpaRepository<DteCorrelativo, DteCorrelativoId> {
    DteCorrelativo findByTipoDteAndCodEstableAndCodPuntoVenta(String tipoDte, String codEstable, String codPuntoVenta);
}
