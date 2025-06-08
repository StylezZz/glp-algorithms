package com.glp.glpDP1.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para visualización de rutas en el frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaVisualizacionDTO {

    private String id;
    private String codigoCamion;
    private CoordenadaDTO origen;
    private CoordenadaDTO destino;
    private List<CoordenadaDTO> secuenciaNodos;
    private List<CoordenadaDTO> secuenciaParadas;
    private List<PedidoVisualizacionDTO> pedidosAsignados;
    private List<EventoRutaDTO> eventos;
    private double distanciaTotal;
    private double consumoCombustible;
    private boolean completada;
    private boolean cancelada;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinEstimada;
    private LocalDateTime horaFinReal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordenadaDTO {
        private int x;
        private int y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PedidoVisualizacionDTO {
        private String id;
        private String idCliente;
        private CoordenadaDTO ubicacion;
        private double cantidadGLP;
        private LocalDateTime horaRecepcion;
        private LocalDateTime horaLimiteEntrega;
        private LocalDateTime horaEntregaProgramada;
        private LocalDateTime horaEntregaReal;
        private boolean entregado;
        private String camionAsignado;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventoRutaDTO {
        private String tipo;
        private LocalDateTime momento;
        private CoordenadaDTO ubicacion;
        private String descripcion;
    }
}