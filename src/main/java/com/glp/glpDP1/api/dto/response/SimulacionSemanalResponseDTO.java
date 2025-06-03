package com.glp.glpDP1.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para la respuesta completa de simulación semanal con datos estructurados para visualización
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionSemanalResponseDTO {

    // Información general
    private String id;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String duracionSimulacion;

    // Estadísticas generales
    private EstadisticasGeneralesDTO estadisticasGenerales;

    // Rutas estructuradas por día
    private Map<String, List<RutaVisualizacionDTO>> rutasPorDia;

    // Configuración del mapa
    private ConfiguracionMapaDTO configuracionMapa;

    // Resumen de camiones
    private ResumenCamionesDTO resumenCamiones;

    // Métricas de rendimiento
    private MetricasRendimientoDTO metricas;

    // Resultados detallados por día
    private Map<Integer, ResultadoDiaDTO> resultadosPorDia;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasGeneralesDTO {
        private int pedidosTotales;
        private int pedidosAsignados;
        private int pedidosEntregados;
        private int pedidosRetrasados;
        private double porcentajeEntrega;
        private double distanciaTotal;
        private double consumoCombustible;
        private int averiasOcurridas;
        private double calidadSolucionSemanal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfiguracionMapaDTO {
        private int ancho;
        private int alto;
        private List<AlmacenDTO> almacenes;
        private List<BloqueoDTO> bloqueos;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AlmacenDTO {
            private String id;
            private String tipo;
            private RutaVisualizacionDTO.CoordenadaDTO ubicacion;
            private double capacidadMaxima;
            private double nivelActual;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BloqueoDTO {
            private String id;
            private LocalDateTime horaInicio;
            private LocalDateTime horaFin;
            private List<RutaVisualizacionDTO.CoordenadaDTO> nodosBloqueados;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenCamionesDTO {
        private int totalCamiones;
        private List<CamionDTO> camiones;
        private Map<String, Integer> conteoTipos;
        private Map<String, Integer> conteoEstados;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CamionDTO {
            private String codigo;
            private String tipo;
            private String estado;
            private RutaVisualizacionDTO.CoordenadaDTO ubicacionActual;
            private double capacidadTanqueGLP;
            private double nivelGLPActual;
            private double nivelCombustibleActual;
            private boolean enMantenimiento;
            private boolean averiado;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasRendimientoDTO {
        private Map<String, Object> fitness;
        private Map<String, Object> tiempoEjecucion;
        private double eficienciaTiempo;
        private Map<String, Object> metricasDiarias;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultadoDiaDTO {
        private int dia;
        private LocalDateTime fecha;
        private List<RutaVisualizacionDTO> rutas;
        private int pedidosAsignados;
        private int pedidosEntregados;
        private int pedidosRetrasados;
        private double distanciaTotal;
        private double consumoCombustible;
        private int averiasOcurridas;
        private double fitness;
        private long tiempoEjecucionMs;
        private List<String> camionesEnMantenimiento;
        private List<String> camionesConAverias;
    }
}