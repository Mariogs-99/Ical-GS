package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResumenDTO {
    private Double totalNoSuj;
    private Double totalExenta;
    private Double totalGravada;
    private Double subTotalVentas;
    private Double descuNoSuj;
    private Double descuExenta;
    private Double descuGravada;
    private Double porcentajeDescuento;
    private Double totalDescu;
    private List<TributoDTO> tributos;
    private Double subTotal;
    private Double ivaRete1;
    private Double reteRenta;
    private Double montoTotalOperacion;
    private Double totalNoGravado;
    private Double totalPagar;
    private String totalLetras;
    private Double totalIva;
    private Double saldoFavor;
    private Integer condicionOperacion;
    private List<PagoDTO> pagos;
    private String numPagoElectronico;
}

