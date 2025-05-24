package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.SimulacionRequest;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import com.glp.glpDP1.services.impl.SimulacionSemanalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/simulacion-semanal")
@RequiredArgsConstructor
@Slf4j
public class SimulacionSemanalController {

    private final SimulacionSemanalService simulacionSemanalService;
    private final DataRepositoryImpl dataRepository;

    // Almacenamiento en memoria de resultados de simulaciones
    private final Map<String, Map<String, Object>> resultadosSimulacion = new ConcurrentHashMap<>();

    /**
     * Ejecuta una simulación semanal
     */
    @PostMapping("/ejecutar")
    public ResponseEntity<Map<String, Object>> ejecutarSimulacionSemanal(@RequestBody SimulacionRequest request) {
        try {
            log.info("Iniciando simulación semanal");

            // Validar fecha de inicio
            LocalDateTime fechaInicio = request.getFechaInicio();
            if (fechaInicio == null) {
                fechaInicio = LocalDateTime.now();
            }

            // Obtener datos necesarios
            List<Camion> camiones = dataRepository.obtenerCamiones();
            List<Pedido> pedidos = dataRepository.obtenerPedidos();
            Mapa mapa = dataRepository.obtenerMapa();

            if (camiones.isEmpty()) {
                throw new IllegalArgumentException("No hay camiones disponibles");
            }

            if (pedidos.isEmpty()) {
                throw new IllegalArgumentException("No hay pedidos para simular");
            }

            if (mapa == null) {
                throw new IllegalArgumentException("No hay mapa configurado");
            }

            // Ejecutar simulación
            Map<String, Object> resultado = simulacionSemanalService.ejecutarSimulacionSemanal(
                    camiones,
                    pedidos,
                    mapa,
                    fechaInicio);

            // Generar ID para la simulación
            String id = UUID.randomUUID().toString();

            // Guardar resultado en memoria
            resultadosSimulacion.put(id, resultado);

            // Agregar información adicional
            resultado.put("id", id);
            resultado.put("fechaInicio", fechaInicio);
            resultado.put("fechaFin", fechaInicio.plusDays(7));
            resultado.put("duracionSimulacion", "7 días");

            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.error("Error en los parámetros de simulación: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al ejecutar simulación semanal: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al ejecutar simulación", e);
        }
    }

    /**
     * Obtiene los detalles de un día específico de la simulación
     */
    @GetMapping("/detalle-dia/{idSimulacion}/{dia}")
    public ResponseEntity<Map<String, Object>> obtenerDetalleDia(
            @PathVariable String idSimulacion,
            @PathVariable int dia) {

        // Validar existencia de la simulación
        Map<String, Object> resultado = resultadosSimulacion.get(idSimulacion);
        if (resultado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró la simulación con ID: " + idSimulacion);
        }

        // Obtener resultados por día
        Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) resultado
                .get("resultadosPorDia");

        if (resultadosPorDia == null || !resultadosPorDia.containsKey(dia)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontraron datos para el día: " + dia);
        }

        return ResponseEntity.ok(resultadosPorDia.get(dia));
    }

    /**
     * Obtiene estadísticas resumidas de la simulación semanal
     */
    @GetMapping("/estadisticas/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(@PathVariable String idSimulacion) {
        // Validar existencia de la simulación
        Map<String, Object> resultado = resultadosSimulacion.get(idSimulacion);
        if (resultado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró la simulación con ID: " + idSimulacion);
        }

        // Extraer estadísticas principales
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("id", idSimulacion);
        estadisticas.put("fechaInicio", resultado.get("fechaInicio"));
        estadisticas.put("fechaFin", resultado.get("fechaFin"));

        // Pedidos totales y completados
        estadisticas.put("pedidosTotales", resultado.get("pedidosTotales"));
        estadisticas.put("pedidosAsignados", resultado.get("pedidosAsignados"));
        estadisticas.put("pedidosEntregados", resultado.get("pedidosEntregados"));
        estadisticas.put("pedidosRetrasados", resultado.get("pedidosRetrasados"));
        estadisticas.put("porcentajeEntrega", resultado.get("porcentajeEntrega"));

        // Estadísticas operativas
        estadisticas.put("distanciaTotal", resultado.get("distanciaTotal"));
        estadisticas.put("consumoCombustible", resultado.get("consumoCombustible"));
        estadisticas.put("averiasOcurridas", resultado.get("averiasOcurridas"));

        // Analizar desglose por día
        Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) resultado
                .get("resultadosPorDia");

        if (resultadosPorDia != null) {
            Map<String, Object> desgloseDiario = new HashMap<>();

            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                Map<String, Object> resumenDia = new HashMap<>();
                resumenDia.put("pedidosAsignados", datosDia.get("pedidosAsignados"));
                resumenDia.put("pedidosEntregados", datosDia.get("pedidosEntregados"));
                resumenDia.put("pedidosRetrasados", datosDia.get("pedidosRetrasados"));
                resumenDia.put("averiasOcurridas", datosDia.get("averiasOcurridas"));

                desgloseDiario.put("Día " + dia, resumenDia);
            }

            estadisticas.put("desgloseDiario", desgloseDiario);
        }

        return ResponseEntity.ok(estadisticas);
    }

    /**
     * Nuevo endpoint para obtener un resumen específico de pedidos por semana
     */
    @GetMapping("/resumen-pedidos/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerResumenPedidos(@PathVariable String idSimulacion) {
        // Validar existencia de la simulación
        Map<String, Object> resultado = resultadosSimulacion.get(idSimulacion);
        if (resultado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró la simulación con ID: " + idSimulacion);
        }

        // Crear resumen específico de pedidos
        Map<String, Object> resumenPedidos = new HashMap<>();

        // Pedidos totales y completados
        int pedidosTotales = (int) resultado.getOrDefault("pedidosTotales", 0);
        int pedidosEntregados = (int) resultado.getOrDefault("pedidosEntregados", 0);
        int pedidosRetrasados = (int) resultado.getOrDefault("pedidosRetrasados", 0);
        double porcentajeEntrega = (double) resultado.getOrDefault("porcentajeEntrega", 0.0);

        resumenPedidos.put("pedidosTotales", pedidosTotales);
        resumenPedidos.put("pedidosEntregados", pedidosEntregados);
        resumenPedidos.put("pedidosNoEntregados", pedidosTotales - pedidosEntregados);
        resumenPedidos.put("pedidosEntregadosATiempo", pedidosEntregados - pedidosRetrasados);
        resumenPedidos.put("pedidosEntregadosConRetraso", pedidosRetrasados);
        resumenPedidos.put("efectividadEntrega", porcentajeEntrega);

        // Obtener desglose por día
        Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) resultado
                .get("resultadosPorDia");

        if (resultadosPorDia != null) {
            Map<String, Integer> pedidosPorDia = new HashMap<>();
            Map<String, Integer> entregasPorDia = new HashMap<>();

            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                int pedidosDia = (int) datosDia.getOrDefault("pedidosAsignados", 0);
                int entregasDia = (int) datosDia.getOrDefault("pedidosEntregados", 0);

                pedidosPorDia.put("Día " + dia, pedidosDia);
                entregasPorDia.put("Día " + dia, entregasDia);
            }

            resumenPedidos.put("pedidosPorDia", pedidosPorDia);
            resumenPedidos.put("entregasPorDia", entregasPorDia);
        }

        return ResponseEntity.ok(resumenPedidos);
    }

    /**
     * Endpoint optimizado para estadísticas semanales con métricas específicas
     */
    @GetMapping("/metricas-semana/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerMetricasSemana(@PathVariable String idSimulacion) {
        // Validar existencia de la simulación
        Map<String, Object> resultado = resultadosSimulacion.get(idSimulacion);
        if (resultado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró la simulación con ID: " + idSimulacion);
        }

        // Extraer solo las métricas clave requeridas
        Map<String, Object> metricasClave = new HashMap<>();

        // Información general
        metricasClave.put("idSimulacion", idSimulacion);
        metricasClave.put("fechaInicio", resultado.get("fechaInicio"));
        metricasClave.put("fechaFin", resultado.get("fechaFin"));

        // Métricas específicas solicitadas
        double distanciaTotal = (double) resultado.getOrDefault("distanciaTotal", 0.0);
        int pedidosEntregados = (int) resultado.getOrDefault("pedidosEntregados", 0);
        double fitness = 0.0;
        long tiempoEjecucion = 0L;

        // Buscar fitness y tiempo de ejecución en los datos de la simulación
        if (resultado.containsKey("fitness")) {
            fitness = (double) resultado.get("fitness");
        }

        if (resultado.containsKey("tiempoEjecucionMs")) {
            tiempoEjecucion = (long) resultado.get("tiempoEjecucionMs");
        }

        // Añadir métricas clave
        metricasClave.put("distanciaTotal", distanciaTotal);
        metricasClave.put("pedidosEntregados", pedidosEntregados);
        metricasClave.put("fitness", fitness);
        metricasClave.put("tiempoEjecucionMs", tiempoEjecucion);

        // Calcular métricas derivadas
        int pedidosTotales = (int) resultado.getOrDefault("pedidosTotales", 0);
        double porcentajeEntrega = pedidosTotales > 0 ? (double) pedidosEntregados / pedidosTotales * 100 : 0.0;

        metricasClave.put("pedidosTotales", pedidosTotales);
        metricasClave.put("pedidosNoEntregados", pedidosTotales - pedidosEntregados);
        metricasClave.put("porcentajeEntrega", porcentajeEntrega);

        // Añadir eficiencia por km
        double eficienciaPorKm = distanciaTotal > 0 ? (double) pedidosEntregados / distanciaTotal : 0.0;
        metricasClave.put("eficienciaPorKm", eficienciaPorKm);

        // Añadir gráfico diario de métricas clave
        Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) resultado
                .get("resultadosPorDia");

        if (resultadosPorDia != null) {
            Map<String, Double> distanciaPorDia = new HashMap<>();
            Map<String, Integer> entregasPorDia = new HashMap<>();
            Map<String, Double> fitnessPorDia = new HashMap<>();

            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                double distanciaDia = (double) datosDia.getOrDefault("distanciaTotal", 0.0);
                int entregasDia = (int) datosDia.getOrDefault("pedidosEntregados", 0);
                double fitnessDia = datosDia.containsKey("fitness") ? (double) datosDia.get("fitness") : 0.0;

                distanciaPorDia.put("Día " + dia, distanciaDia);
                entregasPorDia.put("Día " + dia, entregasDia);
                fitnessPorDia.put("Día " + dia, fitnessDia);
            }

            // Añadir métricas diarias
            Map<String, Object> metricasDiarias = new HashMap<>();
            metricasDiarias.put("distanciaPorDia", distanciaPorDia);
            metricasDiarias.put("entregasPorDia", entregasPorDia);
            metricasDiarias.put("fitnessPorDia", fitnessPorDia);

            metricasClave.put("metricasDiarias", metricasDiarias);
        }

        return ResponseEntity.ok(metricasClave);
    }
}