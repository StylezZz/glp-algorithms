package com.glp.glpDP1.api.dto.request;

public class PedidoRequest {
    private String id;
    private String idCliente;
    private int x;
    private int y;
    private double cantidadGLP;
    private String horaRecepcion;         // formato: "HH:mm"
    private String tiempoLimiteEntrega;   // en horas, ej: "4"

    public PedidoRequest() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getCantidadGLP() {
        return cantidadGLP;
    }

    public void setCantidadGLP(double cantidadGLP) {
        this.cantidadGLP = cantidadGLP;
    }

    public String getHoraRecepcion() {
        return horaRecepcion;
    }

    public void setHoraRecepcion(String horaRecepcion) {
        this.horaRecepcion = horaRecepcion;
    }

    public String getTiempoLimiteEntrega() {
        return tiempoLimiteEntrega;
    }

    public void setTiempoLimiteEntrega(String tiempoLimiteEntrega) {
        this.tiempoLimiteEntrega = tiempoLimiteEntrega;
    }
}
