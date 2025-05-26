package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import com.glp.glpDP1.services.AlgoritmoService;
import com.glp.glpDP1.services.impl.SimulacionTemporalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controlador para gestionar movimientos detallados de camiones
 */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@Slf4j
public class MovimientoController {

    private final AlgoritmoService algoritmoService;
    private final SimulacionTemporalService simulacionTemporalService;
    private final DataRepositoryImpl dataRepository;

    // Cache de movimientos por simulación
    private final Map<String, List<MovimientoCamion>> movimientosCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> fechasInicioCache = new ConcurrentHashMap<>();

    /**
     * Genera movimientos detallados para una simulación existente
     */
    @PostMapping("/generar/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> generarMovimientosDetallados(@PathVariable String idSimulacion) {
        try {
            log.info("Generando movimientos detallados para simulación: {}", idSimulacion);

            // Obtener resultado de la simulación
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(idSimulacion);

            if (resultado == null || resultado.getRutas().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron rutas para la simulación: " + idSimulacion);
            }

            // Obtener mapa
            Mapa mapa = dataRepository.obtenerMapa();
            if (mapa == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se encontró configuración del mapa");
            }

            // Usar hora de inicio de la simulación
            LocalDateTime fechaInicio = resultado.getHoraInicio();
            if (fechaInicio == null) {
                fechaInicio = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
            }

            // Generar movimientos detallados para cada ruta
            List<MovimientoCamion> movimientos = new ArrayList<>();

            for (Ruta ruta : resultado.getRutas()) {
                // Generar movimiento detallado para esta ruta
                ruta.generarMovimientoDetallado(mapa, fechaInicio);

                if (ruta.getMovimientoDetallado() != null) {
                    movimientos.add(ruta.getMovimientoDetallado());
                }
            }

            // Optimizar movimientos para visualización
            List<MovimientoCamion> movimientosOptimizados =
                    simulacionTemporalService.optimizarMovimientosParaVisualizacion(movimientos);

            // Guardar en cache
            movimientosCache.put(idSimulacion, movimientosOptimizados);
            fechasInicioCache.put(idSimulacion, fechaInicio);

            // Preparar respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("idSimulacion", idSimulacion);
            respuesta.put("totalMovimientos", movimientosOptimizados.size());
            respuesta.put("fechaInicio", fechaInicio);
            respuesta.put("fechaFin", calcularFechaFin(movimientosOptimizados));
            respuesta.put("duracionTotalHoras", calcularDuracionTotal(movimientosOptimizados));

            // Estadísticas de movimientos
            Map<String, Object> estadisticas = calcularEstadisticasMovimientos(movimientosOptimizados);
            respuesta.put("estadisticas", estadisticas);

            log.info("Movimientos generados exitosamente: {} movimientos para {} rutas",
                    movimientosOptimizados.size(), resultado.getRutas().size());

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al generar movimientos detallados: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar movimientos detallados", e);
        }
    }

    /**
     * Obtiene las posiciones de todos los camiones en un momento específico
     */
    @GetMapping("/posiciones/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerPosicionesEnMomento(
            @PathVariable String idSimulacion,
            @RequestParam String momento) {

        try {
            List<MovimientoCamion> movimientos = movimientosCache.get(idSimulacion);
            if (movimientos == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron movimientos para la simulación: " + idSimulacion);
            }

            LocalDateTime momentoParsed = LocalDateTime.parse(momento, DateTimeFormatter.ISO_DATE_TIME);

            // Obtener posiciones de todos los camiones
            Map<String, MovimientoCamion.PosicionCamion> posiciones =
                    simulacionTemporalService.obtenerPosicionesCamiones(movimientos, momentoParsed);

            // Convertir a formato JSON amigable
            Map<String, Object> posicionesJson = new HashMap<>();

            for (Map.Entry<String, MovimientoCamion.PosicionCamion> entry : posiciones.entrySet()) {
                String codigoCamion = entry.getKey();
                MovimientoCamion.PosicionCamion posicion = entry.getValue();

                Map<String, Object> posicionJson = new HashMap<>();
                posicionJson.put("ubicacion", Map.of(
                        "x", posicion.getUbicacion().getX(),
                        "y", posicion.getUbicacion().getY()
                ));
                posicionJson.put("progresoTramo", posicion.getProgresoTramo());
                posicionJson.put("estado", posicion.getEstado().toString());
                posicionJson.put("actividadActual", posicion.getActividadActual());
                posicionJson.put("ultimaActualizacion", posicion.getUltimaActualizacion());

                posicionesJson.put(codigoCamion, posicionJson);
            }

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("momento", momentoParsed);
            respuesta.put("totalCamiones", posicionesJson.size());
            respuesta.put("posiciones", posicionesJson);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener posiciones: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener posiciones de camiones", e);
        }
    }

    /**
     * Obtiene el movimiento completo de un camión específico
     */
    @GetMapping("/camion/{idSimulacion}/{codigoCamion}")
    public ResponseEntity<Map<String, Object>> obtenerMovimientoCamion(
            @PathVariable String idSimulacion,
            @PathVariable String codigoCamion) {

        try {
            List<MovimientoCamion> movimientos = movimientosCache.get(idSimulacion);
            if (movimientos == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron movimientos para la simulación: " + idSimulacion);
            }

            MovimientoCamion movimientoCamion = movimientos.stream()
                    .filter(m -> m.getCodigoCamion().equals(codigoCamion))
                    .findFirst()
                    .orElse(null);

            if (movimientoCamion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró movimiento para el camión: " + codigoCamion);
            }

            // Convertir a formato JSON
            Map<String, Object> movimientoJson = convertirMovimientoAJson(movimientoCamion);

            return ResponseEntity.ok(movimientoJson);

        } catch (Exception e) {
            log.error("Error al obtener movimiento del camión: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener movimiento del camión", e);
        }
    }

    /**
     * Obtiene los timestamps para animación de una simulación
     */
    @GetMapping("/timestamps/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerTimestampsAnimacion(
            @PathVariable String idSimulacion,
            @RequestParam(defaultValue = "60") int intervalMinutos) {

        try {
            LocalDateTime fechaInicio = fechasInicioCache.get(idSimulacion);
            if (fechaInicio == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró fecha de inicio para la simulación: " + idSimulacion);
            }

            List<MovimientoCamion> movimientos = movimientosCache.get(idSimulacion);
            if (movimientos == null || movimientos.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron movimientos para la simulación: " + idSimulacion);
            }

            // Calcular duración total
            LocalDateTime fechaFin = calcularFechaFin(movimientos);
            int duracionHoras = (int) java.time.Duration.between(fechaInicio, fechaFin).toHours() + 1;

            // Generar timestamps
            List<LocalDateTime> timestamps = simulacionTemporalService
                    .generarTimestampsAnimacion(fechaInicio, duracionHoras);

            // Filtrar según intervalo solicitado
            List<LocalDateTime> timestampsFiltrados = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i += intervalMinutos) {
                timestampsFiltrados.add(timestamps.get(i));
            }

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("fechaInicio", fechaInicio);
            respuesta.put("fechaFin", fechaFin);
            respuesta.put("duracionHoras", duracionHoras);
            respuesta.put("intervalMinutos", intervalMinutos);
            respuesta.put("totalTimestamps", timestampsFiltrados.size());
            respuesta.put("timestamps", timestampsFiltrados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener timestamps: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener timestamps de animación", e);
        }
    }

    /**
     * Obtiene el progreso de todas las rutas en un momento específico
     */
    @GetMapping("/progreso/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerProgresoRutas(
            @PathVariable String idSimulacion,
            @RequestParam String momento) {

        try {
            List<MovimientoCamion> movimientos = movimientosCache.get(idSimulacion);
            if (movimientos == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron movimientos para la simulación: " + idSimulacion);
            }

            LocalDateTime momentoParsed = LocalDateTime.parse(momento, DateTimeFormatter.ISO_DATE_TIME);

            Map<String, Object> progresoRutas = new HashMap<>();
            double progresoTotal = 0.0;
            int rutasActivas = 0;
            int rutasCompletadas = 0;

            for (MovimientoCamion movimiento : movimientos) {
                double progreso = movimiento.calcularProgreso(momentoParsed);

                Map<String, Object> progresoRuta = new HashMap<>();
                progresoRuta.put("progreso", progreso);
                progresoRuta.put("estado", obtenerEstadoMovimiento(movimiento, momentoParsed));
                progresoRuta.put("rutaId", movimiento.getRutaId());

                progresoRutas.put(movimiento.getCodigoCamion(), progresoRuta);

                if (progreso >= 100.0) {
                    rutasCompletadas++;
                } else if (progreso > 0.0) {
                    rutasActivas++;
                }

                progresoTotal += progreso;
            }

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("momento", momentoParsed);
            respuesta.put("progresoPromedio", movimientos.isEmpty() ? 0.0 : progresoTotal / movimientos.size());
            respuesta.put("rutasActivas", rutasActivas);
            respuesta.put("rutasCompletadas", rutasCompletadas);
            respuesta.put("totalRutas", movimientos.size());
            respuesta.put("progresoRutas", progresoRutas);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener progreso: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener progreso de rutas", e);
        }
    }

    /**
     * Limpia el cache de movimientos
     */
    @DeleteMapping("/cache/{idSimulacion}")
    public ResponseEntity<Map<String, Boolean>> limpiarCache(@PathVariable String idSimulacion) {
        movimientosCache.remove(idSimulacion);
        fechasInicioCache.remove(idSimulacion);

        Map<String, Boolean> respuesta = new HashMap<>();
        respuesta.put("cacheEliminado", true);

        return ResponseEntity.ok(respuesta);
    }

    // Métodos auxiliares

    private LocalDateTime calcularFechaFin(List<MovimientoCamion> movimientos) {
        return movimientos.stream()
                .map(MovimientoCamion::getHoraFinEstimada)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().plusHours(8));
    }

    private int calcularDuracionTotal(List<MovimientoCamion> movimientos) {
        LocalDateTime fechaInicio = movimientos.stream()
                .map(MovimientoCamion::getHoraInicio)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime fechaFin = calcularFechaFin(movimientos);

        return (int) java.time.Duration.between(fechaInicio, fechaFin).toHours();
    }

    private Map<String, Object> calcularEstadisticasMovimientos(List<MovimientoCamion> movimientos) {
        Map<String, Object> estadisticas = new HashMap<>();

        int totalPasos = movimientos.stream()
                .mapToInt(m -> m.getPasos().size())
                .sum();

        double promedioPasosPorMovimiento = movimientos.isEmpty() ? 0.0 :
                (double) totalPasos / movimientos.size();

        estadisticas.put("totalPasos", totalPasos);
        estadisticas.put("promedioPasosPorMovimiento", promedioPasosPorMovimiento);
        estadisticas.put("totalMovimientos", movimientos.size());

        // Contar tipos de pasos
        Map<String, Long> tiposPasos = movimientos.stream()
                .flatMap(m -> m.getPasos().stream())
                .collect(Collectors.groupingBy(
                        p -> p.getTipo().toString(),
                        Collectors.counting()
                ));

        estadisticas.put("distribucionTiposPasos", tiposPasos);

        return estadisticas;
    }

    private Map<String, Object> convertirMovimientoAJson(MovimientoCamion movimiento) {
        Map<String, Object> json = new HashMap<>();

        json.put("codigoCamion", movimiento.getCodigoCamion());
        json.put("rutaId", movimiento.getRutaId());
        json.put("horaInicio", movimiento.getHoraInicio());
        json.put("horaFinEstimada", movimiento.getHoraFinEstimada());
        json.put("estado", movimiento.getEstado().toString());
        json.put("totalPasos", movimiento.getPasos().size());

        // Convertir pasos
        List<Map<String, Object>> pasosJson = new ArrayList<>();
        for (MovimientoCamion.PasoMovimiento paso : movimiento.getPasos()) {
            Map<String, Object> pasoJson = new HashMap<>();
            pasoJson.put("ubicacion", Map.of(
                    "x", paso.getUbicacion().getX(),
                    "y", paso.getUbicacion().getY()
            ));
            pasoJson.put("tiempoLlegada", paso.getTiempoLlegada());
            pasoJson.put("tipo", paso.getTipo().toString());
            pasoJson.put("descripcion", paso.getDescripcion());
            pasoJson.put("velocidadPromedio", paso.getVelocidadPromedio());

            if (paso.getPedidoId() != null) {
                pasoJson.put("pedidoId", paso.getPedidoId());
            }
            if (paso.getTiempoParada() > 0) {
                pasoJson.put("tiempoParada", paso.getTiempoParada());
            }

            pasosJson.add(pasoJson);
        }

        json.put("pasos", pasosJson);

        return json;
    }

    private String obtenerEstadoMovimiento(MovimientoCamion movimiento, LocalDateTime momento) {
        MovimientoCamion.PasoMovimiento paso = movimiento.obtenerPasoEnMomento(momento);
        if (paso == null) {
            return "PENDIENTE";
        }

        return switch (paso.getTipo()) {
            case INICIO -> "INICIANDO";
            case MOVIMIENTO -> "EN_RUTA";
            case ENTREGA -> "ENTREGANDO";
            case RECARGA_GLP -> "RECARGANDO_GLP";
            case RECARGA_COMBUSTIBLE -> "RECARGANDO_COMBUSTIBLE";
            case AVERIA -> "AVERIADO";
            case FIN -> "COMPLETADO";
            default -> "EN_RUTA";
        };
    }
}