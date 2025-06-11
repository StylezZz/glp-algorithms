package com.glp.glpDP1.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el movimiento detallado paso a paso de un camión
 */
@Getter @Setter
public class MovimientoCamion {

    private final String codigoCamion;
    private final String rutaId;
    private final List<PasoMovimiento> pasos;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinEstimada;
    private EstadoMovimiento estado;
    private int pasoActual;

    public MovimientoCamion(String codigoCamion, String rutaId) {
        this.codigoCamion = codigoCamion;
        this.rutaId = rutaId;
        this.pasos = new ArrayList<>();
        this.estado = EstadoMovimiento.PENDIENTE;
        this.pasoActual = 0;
    }

    /**
     * Añade un paso de movimiento a la secuencia
     */
    public void agregarPaso(PasoMovimiento paso) {
        pasos.add(paso);
    }

    /**
     * Obtiene el paso actual basado en un momento específico
     */
    public PasoMovimiento obtenerPasoEnMomento(LocalDateTime momento) {
        if (pasos.isEmpty() || momento.isBefore(horaInicio)) {
            return null;
        }

        for (int i = 0; i < pasos.size(); i++) {
            PasoMovimiento paso = pasos.get(i);
            if (momento.isBefore(paso.getTiempoLlegada()) ||
                    (i == pasos.size() - 1)) {
                return paso;
            }
        }

        return pasos.get(pasos.size() - 1);
    }

    /**
     * Obtiene la posición interpolada del camión en un momento específico
     */
    public PosicionCamion obtenerPosicionEnMomento(LocalDateTime momento) {
        if (pasos.isEmpty() || momento.isBefore(horaInicio)) {
            return new PosicionCamion(pasos.isEmpty() ? new Ubicacion(0, 0) : pasos.get(0).getUbicacion(),
                    0.0, EstadoMovimiento.PENDIENTE);
        }

        // Encontrar el segmento actual
        for (int i = 0; i < pasos.size() - 1; i++) {
            PasoMovimiento pasoActual = pasos.get(i);
            PasoMovimiento pasoSiguiente = pasos.get(i + 1);

            if (momento.isAfter(pasoActual.getTiempoLlegada()) &&
                    momento.isBefore(pasoSiguiente.getTiempoLlegada())) {

                // Interpolar posición entre dos puntos
                return interpolarPosicion(pasoActual, pasoSiguiente, momento);
            }
        }

        // Si está después del último paso
        PasoMovimiento ultimoPaso = pasos.get(pasos.size() - 1);
        return new PosicionCamion(ultimoPaso.getUbicacion(), 100.0,
                momento.isAfter(ultimoPaso.getTiempoLlegada()) ?
                        EstadoMovimiento.COMPLETADO : EstadoMovimiento.EN_MOVIMIENTO);
    }

    /**
     * Interpola la posición entre dos pasos
     */
    private PosicionCamion interpolarPosicion(PasoMovimiento paso1, PasoMovimiento paso2, LocalDateTime momento) {
        long tiempoTotal = java.time.Duration.between(paso1.getTiempoLlegada(), paso2.getTiempoLlegada()).toSeconds();
        long tiempoTranscurrido = java.time.Duration.between(paso1.getTiempoLlegada(), momento).toSeconds();

        if (tiempoTotal == 0) {
            return new PosicionCamion(paso1.getUbicacion(), 0.0, EstadoMovimiento.EN_MOVIMIENTO);
        }

        double progreso = (double) tiempoTranscurrido / tiempoTotal;
        progreso = Math.max(0.0, Math.min(1.0, progreso)); // Clamp entre 0 y 1

        // Interpolación lineal
        int x = (int) (paso1.getUbicacion().getX() +
                (paso2.getUbicacion().getX() - paso1.getUbicacion().getX()) * progreso);
        int y = (int) (paso1.getUbicacion().getY() +
                (paso2.getUbicacion().getY() - paso1.getUbicacion().getY()) * progreso);

        Ubicacion posicionInterpolada = new Ubicacion(x, y);

        return new PosicionCamion(posicionInterpolada, progreso * 100, EstadoMovimiento.EN_MOVIMIENTO);
    }

    /**
     * Calcula el progreso total de la ruta en porcentaje
     */
    public double calcularProgreso(LocalDateTime momento) {
        if (pasos.isEmpty() || momento.isBefore(horaInicio)) {
            return 0.0;
        }

        if (horaFinEstimada != null && momento.isAfter(horaFinEstimada)) {
            return 100.0;
        }

        long tiempoTotal = java.time.Duration.between(horaInicio, horaFinEstimada).toSeconds();
        long tiempoTranscurrido = java.time.Duration.between(horaInicio, momento).toSeconds();

        if (tiempoTotal == 0) return 100.0;

        return Math.min(100.0, (double) tiempoTranscurrido / tiempoTotal * 100);
    }

    public enum EstadoMovimiento {
        PENDIENTE,
        EN_MOVIMIENTO,
        ENTREGANDO,
        RECARGANDO,
        AVERIADO,
        COMPLETADO,
        CANCELADO
    }

    /**
     * Representa un paso específico en el movimiento del camión
     */
    @Getter @Setter
    public static class PasoMovimiento {
        private final Ubicacion ubicacion;
        private final LocalDateTime tiempoLlegada;
        private final TipoPaso tipo;
        private final String descripcion;
        private final double velocidadPromedio; // km/h para este tramo
        private String pedidoId; // Si es una entrega
        private double tiempoParada; // Minutos de parada en este punto

        public PasoMovimiento(Ubicacion ubicacion, LocalDateTime tiempoLlegada,
                              TipoPaso tipo, String descripcion) {
            this.ubicacion = ubicacion;
            this.tiempoLlegada = tiempoLlegada;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.velocidadPromedio = 50.0; // Default
            this.tiempoParada = 0.0;
        }

        public PasoMovimiento(Ubicacion ubicacion, LocalDateTime tiempoLlegada,
                              TipoPaso tipo, String descripcion, String pedidoId, double tiempoParada) {
            this(ubicacion, tiempoLlegada, tipo, descripcion);
            this.pedidoId = pedidoId;
            this.tiempoParada = tiempoParada;
        }

        public enum TipoPaso {
            INICIO,
            MOVIMIENTO,
            ENTREGA,
            RECARGA_GLP,
            RECARGA_COMBUSTIBLE,
            PARADA_MANTENIMIENTO,
            AVERIA,
            FIN
        }
    }

    /**
     * Representa la posición actual de un camión con contexto
     */
    @Getter @Setter
    public static class PosicionCamion {
        private final Ubicacion ubicacion;
        private final double progresoTramo; // 0-100% del tramo actual
        private final EstadoMovimiento estado;
        private String actividadActual;
        private LocalDateTime ultimaActualizacion;

        public PosicionCamion(Ubicacion ubicacion, double progresoTramo, EstadoMovimiento estado) {
            this.ubicacion = ubicacion;
            this.progresoTramo = progresoTramo;
            this.estado = estado;
            this.ultimaActualizacion = LocalDateTime.now();
        }
    }
}