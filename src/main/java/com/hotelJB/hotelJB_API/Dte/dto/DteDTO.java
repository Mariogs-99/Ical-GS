package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;

import java.util.List;

@Data
public class DteDTO {
    private IdentificacionDTO identificacion;
    private Object documentoRelacionado;
    private EmisorDTO emisor;
    private ReceptorDTO receptor;
    private Object otrosDocumentos;
    private Object ventaTercero;
    private List<CuerpoDocumentoDTO> cuerpoDocumento;
    private ResumenDTO resumen;
    private ExtensionDTO extension;
    private Object apendice;
}
