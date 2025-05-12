package com.glp.glpDP1.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Clase que representa los detalles de un trasvase de GLP entre camiones
 * durante una avería
 */
@Getter
@AllArgsConstructor
public class DetalleTrasvase {
    private final String camionOrigen;
    private final String camionDestino;
    private final Ubicacion ubicacion;
    private final LocalDateTime momentoInicio;
    private final LocalDateTime momentoFin;
    private final List<Pedido> pedidosTransferidos;
    private final double cantidadGLP;
    
    /**
     * Calcula la duración del trasvase en minutos
     */
    public long getDuracionMinutos() {
        return java.time.Duration.between(momentoInicio, momentoFin).toMinutes();
    }
    
    @Override
    public String toString() {
        return String.format("Trasvase de %s a %s en %s: %.2f m³, %d pedidos",
                camionOrigen, camionDestino, ubicacion, cantidadGLP, pedidosTransferidos.size());
    }
}