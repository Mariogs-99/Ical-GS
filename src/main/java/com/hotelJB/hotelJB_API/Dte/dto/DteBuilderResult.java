package com.hotelJB.hotelJB_API.Dte.dto;

import lombok.Getter;

import java.util.Map;

@Getter
public class DteBuilderResult {

    private final DteRequestDTO dteRequest;
    private final Map<String, Object> jasperParams;
    private String selloRecibido;

    public DteBuilderResult(DteRequestDTO dteRequest, Map<String, Object> jasperParams) {
        this.dteRequest = dteRequest;
        this.jasperParams = jasperParams;
        this.selloRecibido = ""; // Inicializar para evitar null
        this.jasperParams.put("selloRecibido", ""); // Placeholder inicial
    }

    public void setSelloRecibido(String selloRecibido) {
        this.selloRecibido = selloRecibido != null ? selloRecibido : "";
        this.jasperParams.put("selloRecibido", this.selloRecibido);
    }


    public void addJasperParam(String key, Object value) {
        this.jasperParams.put(key, value);
    }
}
