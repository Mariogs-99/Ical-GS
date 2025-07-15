package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Data;

import java.util.List;

@Data
public class CuerpoDocumentoDTO {
    private int numItem;
    private int tipoItem;
    private String numeroDocumento;
    private Double cantidad;
    private String codigo;
    private String codTributo;
    private Integer uniMedida;
    private String descripcion;
    private Double precioUni;
    private Double montoDescu;
    private Double ventaNoSuj;
    private Double ventaExenta;
    private Double ventaGravada;
    private List<String> tributos;
    private Double psv;
    private Double noGravado;
    private Double ivaItem;
}
