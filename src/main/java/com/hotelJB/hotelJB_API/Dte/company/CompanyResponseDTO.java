package com.hotelJB.hotelJB_API.Dte.company;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyResponseDTO {
    private String name;
    private String nombreComercial;
    private String correo;
    private String telefono;
    private String direccion;
    private String nit;
    private String nrc;
    private String departamento;
    private String municipio;
    private boolean dteEnabled;
    private String codEstableMh;
    private String codEstable;
    private String codPuntoVentaMh;
    private String codPuntoVenta;

}
