package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TributoDTO {
    private String codigo;
    private String descripcion;
    private Double valor;
}
