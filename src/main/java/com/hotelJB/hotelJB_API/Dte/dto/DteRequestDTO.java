package com.hotelJB.hotelJB_API.Dte.dto;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@JsonPropertyOrder({
        "identificacion",
        "emisor",
        "receptor",
        "cuerpoDocumento",
        "resumen"
})
public class DteRequestDTO {
    private IdentificacionDTO Identificacion;
    private EmisorDTO Emisor;
    private ReceptorDTO Receptor;
    private List<CuerpoDocumentoDTO> CuerpoDocumento;
    private ResumenDTO Resumen;
    // getters y setters
}
