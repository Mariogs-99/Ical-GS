package com.hotelJB.hotelJB_API.Dte.dto;




import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ResumenDTO {
    private Double TotalGravada;
    private Double SubTotalVentas;
    private Double Iva;
    private Double TotalPagar;
    // getters y setters
}
