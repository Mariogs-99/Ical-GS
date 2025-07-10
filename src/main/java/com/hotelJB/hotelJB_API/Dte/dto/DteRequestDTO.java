package com.hotelJB.hotelJB_API.Dte.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import java.util.List;

@Data
@JsonPropertyOrder({
        "identificacion",
        "documentoRelacionado",
        "emisor",
        "receptor",
        "otrosDocumentos",
        "ventaTercero",
        "cuerpoDocumento",
        "resumen",
        "extension",
        "apendice"
})
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DteRequestDTO {
    private IdentificacionDTO identificacion;
    private Object documentoRelacionado = null;
    private EmisorDTO emisor;
    private ReceptorDTO receptor;
    private Object otrosDocumentos = null;
    private Object ventaTercero = null;
    private List<CuerpoDocumentoDTO> cuerpoDocumento;
    private ResumenDTO resumen;
    private ExtensionDTO extension;
    private Object apendice = null;
}

