package com.hotelJB.hotelJB_API.Dte.conf;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dte_correlativo")
@IdClass(DteCorrelativoId.class)
public class DteCorrelativo {

    @Id
    @Column(name = "tipo_dte")
    private String tipoDte;

    @Id
    @Column(name = "cod_estable")
    private String codEstable;

    @Id
    @Column(name = "cod_punto_venta")
    private String codPuntoVenta;

    @Column(name = "correlativo")
    private Long correlativo;
}
