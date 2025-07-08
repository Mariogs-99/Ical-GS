package com.hotelJB.hotelJB_API.Dte.dto;



import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ReceptorDTO {
    private String TipoDocumento;
    private String NumDocumento;
    private String Nombre;
    // más campos opcionales
    // getters y setters
}

