package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;

@Data
public class IdentificacionDTO {
    private int version;
    private String ambiente;
    private String tipoDte;
    private String numeroControl;
    private String codigoGeneracion;
    private int tipoModelo;
    private int tipoOperacion;
    private String tipoMoneda;
    private String fecEmi;
    private String horEmi;
    private String tipoContingencia;
    private String motivoContin;
}
