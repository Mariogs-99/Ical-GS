package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;

@Data
public class EmisorDTO {
    private String nit;
    private String nrc;
    private String nombre;
    private String codActividad;
    private String descActividad;
    private String nombreComercial;
    private String tipoEstablecimiento;
    private DireccionDTO direccion;
    private String telefono;
    private String correo;
    private String codEstableMH;
    private String codEstable;
    private String codPuntoVentaMH;
    private String codPuntoVenta;
}


