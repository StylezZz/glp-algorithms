package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.MovimientoCamion.PasoMovimiento;
import com.glp.glpDP1.domain.MovimientoCamion.EstadoMovimiento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para generar simulaciones temporales detalladas de movimientos de camiones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulacionTemporalService {

    private final AveriaService averiaService;

    /**
     * Genera movimientos detallados para todas las rutas de un día
     */
    public List<MovimientoCamion> generarMovimientosDetallados(
            List<Ruta> rutas,
            Mapa mapa,
            LocalDateTime fechaInicio) {

        List<MovimientoCamion> movimientos = new ArrayList<>();

        for (Ruta ruta : rutas) {
            MovimientoCamion movimiento = generarMovimientoRuta(ruta, mapa, fechaInicio);
            if (movimiento != null) {
                movimientos.add(movimiento);
            }
        }

        log.info("Generados {} movimientos detallados para {} rutas",
                movimientos.size(), rutas.size());

        return movimientos;
    }

    /**
     * Genera el movimiento detallado para una ruta específica
     */
    private MovimientoCamion generarMovimientoRuta(Ruta ruta, Mapa mapa, LocalDateTime fechaInicio) {
        if (ruta.getSecuenciaNodos().isEmpty()) {
            log.warn("Ruta {} no tiene secuencia de nodos", ruta.getId());
            return null;
        }

        MovimientoCamion movimiento = new MovimientoCamion(ruta.getCodigoCamion(), ruta.getId());
        movimiento.setHoraInicio(fechaInicio);

        LocalDateTime tiempoActual = fechaInicio;
        Ubicacion ubicacionActual = ruta.getOrigen();

        // Agregar paso inicial
        movimiento.agregarPaso(new PasoMovimiento(
                ubicacionActual,
                tiempoActual,
                PasoMovimiento.TipoPaso.INICIO,
                "Inicio de ruta para camión " + ruta.getCodigoCamion()
        ));

        // Procesar cada segmento de la ruta
        for (int i = 0; i < ruta.getSecuenciaNodos().size(); i++) {
            Ubicacion siguienteNodo = ruta.getSecuenciaNodos().get(i);

            // Generar movimiento paso a paso hasta el siguiente nodo
            List<PasoMovimiento> pasosSegmento = generarMovimientoDetallado(
                    ubicacionActual, siguienteNodo, tiempoActual, mapa, ruta.getCodigoCamion());

            // Añadir todos los pasos del segmento excepto el primero (para evitar duplicados)
            for (int j = 1; j < pasosSegmento.size(); j++) {
                movimiento.agregarPaso(pasosSegmento.get(j));
            }

            // Actualizar tiempo y ubicación
            if (!pasosSegmento.isEmpty()) {
                PasoMovimiento ultimoPaso = pasosSegmento.get(pasosSegmento.size() - 1);
                tiempoActual = ultimoPaso.getTiempoLlegada();
                ubicacionActual = ultimoPaso.getUbicacion();
            }

            // Verificar si hay una entrega en este nodo
            Pedido pedidoEnNodo = encontrarPedidoEnUbicacion(ruta.getPedidosAsignados(), siguienteNodo);
            if (pedidoEnNodo != null) {
                // Agregar tiempo de entrega (15 minutos por defecto)
                tiempoActual = tiempoActual.plusMinutes(15);

                movimiento.agregarPaso(new PasoMovimiento(
                        siguienteNodo,
                        tiempoActual,
                        PasoMovimiento.TipoPaso.ENTREGA,
                        "Entrega para cliente " + pedidoEnNodo.getIdCliente(),
                        pedidoEnNodo.getId(),
                        15.0
                ));
            }

            // Verificar si hay averías programadas en este punto
            verificarYAgregarAverias(movimiento, ruta.getCodigoCamion(), tiempoActual, siguienteNodo, i, ruta.getSecuenciaNodos().size());
        }

        // Agregar regreso al destino final si es diferente
        if (ruta.getDestino() != null && !ubicacionActual.equals(ruta.getDestino())) {
            List<PasoMovimiento> pasosRegreso = generarMovimientoDetallado(
                    ubicacionActual, ruta.getDestino(), tiempoActual, mapa, ruta.getCodigoCamion());

            for (int j = 1; j < pasosRegreso.size(); j++) {
                movimiento.agregarPaso(pasosRegreso.get(j));
            }

            if (!pasosRegreso.isEmpty()) {
                tiempoActual = pasosRegreso.get(pasosRegreso.size() - 1).getTiempoLlegada();
            }
        }

        // Agregar paso final
        movimiento.agregarPaso(new PasoMovimiento(
                ruta.getDestino() != null ? ruta.getDestino() : ubicacionActual,
                tiempoActual,
                PasoMovimiento.TipoPaso.FIN,
                "Fin de ruta para camión " + ruta.getCodigoCamion()
        ));

        movimiento.setHoraFinEstimada(tiempoActual);
        movimiento.setEstado(EstadoMovimiento.PENDIENTE);

        log.info("Movimiento generado para camión {}: {} pasos, duración {} minutos",
                ruta.getCodigoCamion(),
                movimiento.getPasos().size(),
                ChronoUnit.MINUTES.between(fechaInicio, tiempoActual));

        return movimiento;
    }

    /**
     * Genera movimiento detallado paso a paso entre dos ubicaciones usando A*
     */
    private List<PasoMovimiento> generarMovimientoDetallado(
            Ubicacion origen,
            Ubicacion destino,
            LocalDateTime tiempoInicio,
            Mapa mapa,
            String codigoCamion) {

        List<PasoMovimiento> pasos = new ArrayList<>();

        // Usar el algoritmo A* del mapa para encontrar la ruta
        List<Ubicacion> rutaDetallada = mapa.encontrarRuta(origen, destino, tiempoInicio);

        if (rutaDetallada.isEmpty()) {
            log.warn("No se encontró ruta de {} a {} para camión {}", origen, destino, codigoCamion);
            return pasos;
        }

        LocalDateTime tiempoActual = tiempoInicio;
        double velocidadPromedio = 50.0; // km/h

        // Generar paso para cada nodo en la ruta
        for (int i = 0; i < rutaDetallada.size(); i++) {
            Ubicacion ubicacion = rutaDetallada.get(i);

            if (i == 0) {
                // Primer nodo (origen)
                pasos.add(new PasoMovimiento(
                        ubicacion,
                        tiempoActual,
                        PasoMovimiento.TipoPaso.MOVIMIENTO,
                        "Posición inicial"
                ));
            } else {
                // Calcular tiempo de viaje desde el nodo anterior
                Ubicacion nodoAnterior = rutaDetallada.get(i - 1);
                int distancia = nodoAnterior.distanciaA(ubicacion);

                // Tiempo de viaje en minutos
                long minutosViaje = Math.round((double) distancia / velocidadPromedio * 60);
                tiempoActual = tiempoActual.plusMinutes(minutosViaje);

                PasoMovimiento.TipoPaso tipoPaso = (i == rutaDetallada.size() - 1) ?
                        PasoMovimiento.TipoPaso.MOVIMIENTO : PasoMovimiento.TipoPaso.MOVIMIENTO;

                pasos.add(new PasoMovimiento(
                        ubicacion,
                        tiempoActual,
                        tipoPaso,
                        String.format("Movimiento a (%d,%d)", ubicacion.getX(), ubicacion.getY())
                ));
            }
        }

        return pasos;
    }

    /**
     * Encuentra un pedido en una ubicación específica
     */
    private Pedido encontrarPedidoEnUbicacion(List<Pedido> pedidos, Ubicacion ubicacion) {
        return pedidos.stream()
                .filter(p -> p.getUbicacion().equals(ubicacion))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica y agrega averías programadas en el movimiento
     */
    private void verificarYAgregarAverias(MovimientoCamion movimiento, String codigoCamion,
                                          LocalDateTime tiempoActual, Ubicacion ubicacion,
                                          int indiceNodo, int totalNodos) {

        if (averiaService == null) return;

        // Verificar si hay avería programada para este camión en este momento
        if (averiaService.tieneProgramadaAveria(codigoCamion, tiempoActual)) {

            // Determinar si la avería ocurre en este punto de la ruta
            // (usar probabilidad basada en el progreso de la ruta)
            double progresoRuta = (double) indiceNodo / totalNodos;
            boolean averiaEnEstePunto = progresoRuta >= 0.2 && progresoRuta <= 0.8; // Entre 20% y 80% de la ruta

            if (averiaEnEstePunto) {
                // Obtener tipo de incidente
                com.glp.glpDP1.domain.enums.Turno turno = com.glp.glpDP1.domain.enums.Turno.obtenerTurnoPorHora(tiempoActual.getHour());
                com.glp.glpDP1.domain.enums.TipoIncidente tipoIncidente =
                        averiaService.obtenerIncidenteProgramado(codigoCamion, turno);

                if (tipoIncidente != null) {
                    // Agregar paso de avería
                    movimiento.agregarPaso(new PasoMovimiento(
                            ubicacion,
                            tiempoActual.plusMinutes(5), // 5 minutos después del arribo
                            PasoMovimiento.TipoPaso.AVERIA,
                            "Avería tipo " + tipoIncidente + " en camión " + codigoCamion
                    ));

                    log.info("Avería programada agregada: {} en {} para camión {} en ubicación {}",
                            tipoIncidente, tiempoActual, codigoCamion, ubicacion);
                }
            }
        }
    }

    /**
     * Obtiene la posición de todos los camiones en un momento específico
     */
    public Map<String, MovimientoCamion.PosicionCamion> obtenerPosicionesCamiones(
            List<MovimientoCamion> movimientos, LocalDateTime momento) {

        Map<String, MovimientoCamion.PosicionCamion> posiciones = new HashMap<>();

        for (MovimientoCamion movimiento : movimientos) {
            MovimientoCamion.PosicionCamion posicion = movimiento.obtenerPosicionEnMomento(momento);
            if (posicion != null) {
                posiciones.put(movimiento.getCodigoCamion(), posicion);
            }
        }

        return posiciones;
    }

    /**
     * Genera timestamps para animación suave (cada minuto del día)
     */
    public List<LocalDateTime> generarTimestampsAnimacion(LocalDateTime fechaInicio, int duracionHoras) {
        List<LocalDateTime> timestamps = new ArrayList<>();

        LocalDateTime actual = fechaInicio;
        LocalDateTime fin = fechaInicio.plusHours(duracionHoras);

        while (actual.isBefore(fin)) {
            timestamps.add(actual);
            actual = actual.plusMinutes(1); // Cada minuto para animación suave
        }

        return timestamps;
    }

    /**
     * Optimiza los movimientos eliminando pasos redundantes para mejorar performance
     */
    public List<MovimientoCamion> optimizarMovimientosParaVisualizacion(List<MovimientoCamion> movimientos) {
        return movimientos.stream()
                .map(this::optimizarMovimientoIndividual)
                .collect(Collectors.toList());
    }

    private MovimientoCamion optimizarMovimientoIndividual(MovimientoCamion movimiento) {
        List<PasoMovimiento> pasosOptimizados = new ArrayList<>();
        List<PasoMovimiento> pasosOriginales = movimiento.getPasos();

        if (pasosOriginales.isEmpty()) {
            return movimiento;
        }

        // Siempre incluir el primer paso
        pasosOptimizados.add(pasosOriginales.get(0));

        // Filtrar pasos: mantener solo cambios significativos de dirección, entregas, y eventos importantes
        for (int i = 1; i < pasosOriginales.size() - 1; i++) {
            PasoMovimiento pasoAnterior = pasosOriginales.get(i - 1);
            PasoMovimiento pasoActual = pasosOriginales.get(i);
            PasoMovimiento pasoSiguiente = pasosOriginales.get(i + 1);

            // Mantener si es un evento importante (no solo movimiento)
            if (pasoActual.getTipo() != PasoMovimiento.TipoPaso.MOVIMIENTO) {
                pasosOptimizados.add(pasoActual);
                continue;
            }

            // Mantener si hay cambio significativo de dirección
            if (hayCambioSignificativoDirection(pasoAnterior, pasoActual, pasoSiguiente)) {
                pasosOptimizados.add(pasoActual);
            }
        }

        // Siempre incluir el último paso
        if (pasosOriginales.size() > 1) {
            pasosOptimizados.add(pasosOriginales.get(pasosOriginales.size() - 1));
        }

        // Crear nuevo movimiento optimizado
        MovimientoCamion movimientoOptimizado = new MovimientoCamion(
                movimiento.getCodigoCamion(),
                movimiento.getRutaId()
        );

        movimientoOptimizado.setHoraInicio(movimiento.getHoraInicio());
        movimientoOptimizado.setHoraFinEstimada(movimiento.getHoraFinEstimada());
        movimientoOptimizado.setEstado(movimiento.getEstado());

        for (PasoMovimiento paso : pasosOptimizados) {
            movimientoOptimizado.agregarPaso(paso);
        }

        log.debug("Movimiento optimizado para {}: {} -> {} pasos",
                movimiento.getCodigoCamion(),
                pasosOriginales.size(),
                pasosOptimizados.size());

        return movimientoOptimizado;
    }

    /**
     * Detecta cambios significativos de dirección para optimización
     */
    private boolean hayCambioSignificativoDirection(PasoMovimiento anterior, PasoMovimiento actual, PasoMovimiento siguiente) {
        // Calcular vectores de dirección
        int dx1 = actual.getUbicacion().getX() - anterior.getUbicacion().getX();
        int dy1 = actual.getUbicacion().getY() - anterior.getUbicacion().getY();

        int dx2 = siguiente.getUbicacion().getX() - actual.getUbicacion().getX();
        int dy2 = siguiente.getUbicacion().getY() - actual.getUbicacion().getY();

        // Si hay cambio de dirección (no van en línea recta)
        return !(dx1 == dx2 && dy1 == dy2);
    }

    /**
     * Configura bloqueos en el mapa para el período de simulación
     */
    public void configurarBloqueosMapa(Mapa mapa, LocalDateTime fechaInicio, int duracionDias) {
        LocalDateTime fechaFin = fechaInicio.plusDays(duracionDias);
        mapa.filtrarBloqueosParaFecha(fechaInicio.toLocalDate(), fechaFin.toLocalDate());

        log.info("Bloqueos configurados para período {} - {}",
                fechaInicio.toLocalDate(), fechaFin.toLocalDate());
    }
}