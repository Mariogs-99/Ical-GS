package com.hotelJB.hotelJB_API.Dte.dto;




import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CuerpoDocumentoDTO {
    private String NumItem;
    private String Descripcion;
    private Integer Cantidad;
    private Double PrecioUni;
    private Double VentaGravada;
    // más campos opcionales
    // getters y setters
}

