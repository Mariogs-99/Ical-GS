package com.hotelJB.hotelJB_API.Dte.conf;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DteCorrelativoId implements Serializable {
    private String tipoDte;
    private String codEstable;
    private String codPuntoVenta;
}
