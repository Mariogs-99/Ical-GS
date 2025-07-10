package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DteResponse {
    private boolean exitoso;
    private String estado;
    private String mensaje;
    private String codigoGeneracion;
    private String selloRecepcion;
    private List<String> observaciones;
    private String responseBody;
}
