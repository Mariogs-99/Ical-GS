package com.hotelJB.hotelJB_API.Dte.dto;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class IdentificacionDTO {
    private String Version;
    private String Ambiente;
    private String TipoDte;
    private String NumeroControl;
    private String CodigoGeneracion;

}
