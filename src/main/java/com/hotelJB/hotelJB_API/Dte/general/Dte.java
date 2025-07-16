package com.hotelJB.hotelJB_API.Dte.general;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "dte")
public class Dte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dteId;

    private String reservationCode;
    private String numeroControl;
    private String codigoGeneracion;
    private String tipoDte;
    private String estado;
    private LocalDateTime fechaGeneracion;

    @Lob
    private String dteJson;

    @Lob
    private String respuestaHaciendaJson;


}

