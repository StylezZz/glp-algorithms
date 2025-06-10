package com.glp.glpDP1.api.dto.websocket;

import com.glp.glpDP1.domain.Ubicacion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EstadoSimulacionResponse {
    private LocalDateTime momentoSimulacion;
    private List<EstadoCamionInterval> estadoCamiones;
    private List<EstadoPedidoSimulacion> pedidosPendientes;
    private List<EventoReciente> eventosRecientes;
    private List<BloqueoActivo> bloqueosActivos;
    private MetricasGenerales metricas;
    private boolean simulacionActiva;

    @Data
    public static class EstadoCamionInterval {
        private String codigo;
        private String estado; // "EN_RUTA", "ENTREGANDO", "AVERIADO", etc.
        private Ubicacion posicionActual;
        private List<Ubicacion> rutaProximos15Min; // Nodos que recorrerá en 15 min
        private double progresoPorcentaje;
        private String actividadActual;
        private LocalDateTime proximaEntrega;
        private double combustibleRestante;
        private double glpRestante;
    }

    @Data
    public static class EstadoPedidoSimulacion {
        private String id;
        private String idCliente;
        private Ubicacion ubicacion;
        private double cantidadGLP;
        private LocalDateTime horaLimiteEntrega;
        private String estadoEntrega; // "PENDIENTE", "EN_RUTA", "ENTREGADO"
        private String camionAsignado;
        private LocalDateTime horaEntregaEstimada;
        private boolean urgente;
    }

    @Data
    public static class EventoReciente {
        private LocalDateTime momento;
        private String tipo;
        private String descripcion;
        private String camionInvolucrado;
        private Ubicacion ubicacion;
    }

    @Data
    public static class MetricasGenerales {
        private int pedidosTotales;
        private int pedidosEntregados;
        private int pedidosPendientes;
        private int camionesActivos;
        private int camionesAveriados;
        private double porcentajeCompletado;
        private double distanciaRecorridaTotal;
        private double combustibleConsumido;
    }

    @Data
    public static class BloqueoActivo {
        private String id;
        private LocalDateTime horaInicio;
        private LocalDateTime horaFin;
        private List<Ubicacion> nodosBloqueados;
        private boolean activoEnIntervalo;
        private String descripcion;
    }
}