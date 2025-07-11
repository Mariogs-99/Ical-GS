package com.hotelJB.hotelJB_API.Dte.dto;

import java.util.Map;

public class DteBuilderResult {

    private final DteRequestDTO dteRequest;
    private final Map<String, Object> jasperParams;

    public DteBuilderResult(DteRequestDTO dteRequest, Map<String, Object> jasperParams) {
        this.dteRequest = dteRequest;
        this.jasperParams = jasperParams;
    }

    public DteRequestDTO getDteRequest() {
        return dteRequest;
    }

    public Map<String, Object> getJasperParams() {
        return jasperParams;
    }
}
