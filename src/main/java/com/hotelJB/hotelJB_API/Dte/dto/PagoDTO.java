package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;

@Data
public class PagoDTO {
    private String codigo;
    private Double montoPago;
    private String referencia;
    private String plazo;
    private String periodo;
}
