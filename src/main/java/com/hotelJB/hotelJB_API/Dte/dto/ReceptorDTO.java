package com.hotelJB.hotelJB_API.Dte.dto;



import lombok.Data;


@Data
public class ReceptorDTO {
    private String tipoDocumento;
    private String numDocumento;
    private String nombre;
    private String codActividad;
    private String descActividad;
    private DireccionDTO direccion;
    private String telefono;
    private String correo;
    private String nrc;
}



