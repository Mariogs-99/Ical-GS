package com.hotelJB.hotelJB_API.Dte.dto;

public class DteResponse {

    private boolean exitoso;
    private String estado;
    private String mensaje;
    private String codigoGeneracion;
    private String selloRecepcion;
    private String observaciones;
    private String responseBody;

    // Constructor con argumentos
    public DteResponse(boolean exitoso,
                       String estado,
                       String mensaje,
                       String codigoGeneracion,
                       String selloRecepcion,
                       String observaciones,
                       String responseBody) {
        this.exitoso = exitoso;
        this.estado = estado;
        this.mensaje = mensaje;
        this.codigoGeneracion = codigoGeneracion;
        this.selloRecepcion = selloRecepcion;
        this.observaciones = observaciones;
        this.responseBody = responseBody;
    }

    // Getters y setters

    public boolean isExitoso() {
        return exitoso;
    }

    public void setExitoso(boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCodigoGeneracion() {
        return codigoGeneracion;
    }

    public void setCodigoGeneracion(String codigoGeneracion) {
        this.codigoGeneracion = codigoGeneracion;
    }

    public String getSelloRecepcion() {
        return selloRecepcion;
    }

    public void setSelloRecepcion(String selloRecepcion) {
        this.selloRecepcion = selloRecepcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
