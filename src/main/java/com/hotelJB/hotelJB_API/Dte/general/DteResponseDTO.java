package com.hotelJB.hotelJB_API.Dte.general;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DteResponseDTO {
    private Long dteId;
    private String reservationCode;
    private String numeroControl;
    private String codigoGeneracion;
    private String tipoDte;
    private String estado;
    private LocalDateTime fechaGeneracion;
    private String dteJson;
    private String respuestaHaciendaJson;
}
