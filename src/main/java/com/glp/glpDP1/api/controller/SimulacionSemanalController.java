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
     * Ejecuta una simulación semanal con rutas estructuradas para visualización
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

            // Estructurar datos para visualización
            Map<String, Object> respuestaCompleta = estructurarDatosParaVisualizacion(resultado, id, fechaInicio);

            // Guardar resultado en memoria
            resultadosSimulacion.put(id, respuestaCompleta);

            return ResponseEntity.ok(respuestaCompleta);
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
     * Obtiene las rutas específicas para visualización en el mapa
     */
    @GetMapping("/rutas-visualizacion/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerRutasVisualizacion(@PathVariable String idSimulacion) {
        try {
            Map<String, Object> simulacion = resultadosSimulacion.get(idSimulacion);
            if (simulacion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró la simulación con ID: " + idSimulacion);
            }

            // Extraer solo los datos de rutas para visualización
            Map<String, Object> rutasVisualizacion = new HashMap<>();
            rutasVisualizacion.put("id", idSimulacion);
            rutasVisualizacion.put("rutasPorDia", simulacion.get("rutasPorDia"));
            rutasVisualizacion.put("resumenCamiones", simulacion.get("resumenCamiones"));
            rutasVisualizacion.put("configuracionMapa", simulacion.get("configuracionMapa"));
            rutasVisualizacion.put("fechaInicio", simulacion.get("fechaInicio"));
            rutasVisualizacion.put("fechaFin", simulacion.get("fechaFin"));

            return ResponseEntity.ok(rutasVisualizacion);
        } catch (Exception e) {
            log.error("Error al obtener rutas para visualización: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener datos de visualización", e);
        }
    }

    /**
     * Obtiene las rutas de un día específico
     */
    @GetMapping("/rutas-dia/{idSimulacion}/{dia}")
    public ResponseEntity<Map<String, Object>> obtenerRutasDia(
            @PathVariable String idSimulacion,
            @PathVariable int dia) {
        try {
            Map<String, Object> simulacion = resultadosSimulacion.get(idSimulacion);
            if (simulacion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró la simulación con ID: " + idSimulacion);
            }

            Map<String, Object> rutasPorDia = (Map<String, Object>) simulacion.get("rutasPorDia");
            if (rutasPorDia == null || !rutasPorDia.containsKey("dia_" + dia)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontraron rutas para el día: " + dia);
            }

            Map<String, Object> rutasDia = new HashMap<>();
            rutasDia.put("dia", dia);
            rutasDia.put("rutas", rutasPorDia.get("dia_" + dia));
            rutasDia.put("configuracionMapa", simulacion.get("configuracionMapa"));

            return ResponseEntity.ok(rutasDia);
        } catch (Exception e) {
            log.error("Error al obtener rutas del día: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener rutas del día", e);
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

    /**
     * Estructura los datos de la simulación para facilitar la visualización
     */
    private Map<String, Object> estructurarDatosParaVisualizacion(Map<String, Object> resultado, String id, LocalDateTime fechaInicio) {
        Map<String, Object> respuesta = new HashMap<>(resultado);

        // Agregar información básica
        respuesta.put("id", id);
        respuesta.put("fechaInicio", fechaInicio);
        respuesta.put("fechaFin", fechaInicio.plusDays(7));
        respuesta.put("duracionSimulacion", "7 días");

        // Estructurar rutas por día para visualización
        Map<String, Object> rutasPorDia = new HashMap<>();
        Map<String, Object> resumenCamiones = new HashMap<>();

        Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) resultado.get("resultadosPorDia");

        if (resultadosPorDia != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                // Procesar rutas del día
                List<Map<String, Object>> rutasEstructuradas = estructurarRutasDia(datosDia);
                rutasPorDia.put("dia_" + dia, rutasEstructuradas);
            }
        }

        // Obtener configuración del mapa
        Mapa mapa = dataRepository.obtenerMapa();
        Map<String, Object> configuracionMapa = estructurarConfiguracionMapa(mapa);

        // Obtener resumen de camiones
        List<Camion> camiones = dataRepository.obtenerCamiones();
        resumenCamiones = estructurarResumenCamiones(camiones);

        respuesta.put("rutasPorDia", rutasPorDia);
        respuesta.put("resumenCamiones", resumenCamiones);
        respuesta.put("configuracionMapa", configuracionMapa);

        return respuesta;
    }

    /**
     * Estructura las rutas de un día específico para visualización
     */
    private List<Map<String, Object>> estructurarRutasDia(Map<String, Object> datosDia) {
        List<Map<String, Object>> rutasEstructuradas = new java.util.ArrayList<>();

        if (datosDia.containsKey("rutas")) {
            List<com.glp.glpDP1.domain.Ruta> rutas = (List<com.glp.glpDP1.domain.Ruta>) datosDia.get("rutas");

            for (com.glp.glpDP1.domain.Ruta ruta : rutas) {
                Map<String, Object> rutaVisual = new HashMap<>();

                rutaVisual.put("id", ruta.getId());
                rutaVisual.put("codigoCamion", ruta.getCodigoCamion());
                rutaVisual.put("origen", coordenadaToMap(ruta.getOrigen()));
                rutaVisual.put("destino", coordenadaToMap(ruta.getDestino()));

                // Convertir secuencia de nodos
                List<Map<String, Object>> secuenciaNodos = new java.util.ArrayList<>();
                for (com.glp.glpDP1.domain.Ubicacion ubicacion : ruta.getSecuenciaNodos()) {
                    secuenciaNodos.add(coordenadaToMap(ubicacion));
                }
                rutaVisual.put("secuenciaNodos", secuenciaNodos);

                // Convertir pedidos asignados
                List<Map<String, Object>> pedidosVisuales = new java.util.ArrayList<>();
                for (com.glp.glpDP1.domain.Pedido pedido : ruta.getPedidosAsignados()) {
                    Map<String, Object> pedidoVisual = new HashMap<>();
                    pedidoVisual.put("id", pedido.getId());
                    pedidoVisual.put("idCliente", pedido.getIdCliente());
                    pedidoVisual.put("ubicacion", coordenadaToMap(pedido.getUbicacion()));
                    pedidoVisual.put("cantidadGLP", pedido.getCantidadGLP());
                    pedidoVisual.put("entregado", pedido.isEntregado());
                    pedidoVisual.put("horaLimiteEntrega", pedido.getHoraLimiteEntrega());
                    if (pedido.getHoraEntregaReal() != null) {
                        pedidoVisual.put("horaEntregaReal", pedido.getHoraEntregaReal());
                    }
                    pedidosVisuales.add(pedidoVisual);
                }
                rutaVisual.put("pedidosAsignados", pedidosVisuales);

                // Métricas de la ruta
                rutaVisual.put("distanciaTotal", ruta.getDistanciaTotal());
                rutaVisual.put("consumoCombustible", ruta.getConsumoCombustible());
                rutaVisual.put("completada", ruta.isCompletada());
                rutaVisual.put("cancelada", ruta.isCancelada());

                // Eventos de la ruta
                List<Map<String, Object>> eventosVisuales = new java.util.ArrayList<>();
                for (com.glp.glpDP1.domain.EventoRuta evento : ruta.getEventos()) {
                    Map<String, Object> eventoVisual = new HashMap<>();
                    eventoVisual.put("tipo", evento.getTipo().toString());
                    eventoVisual.put("momento", evento.getMomento());
                    eventoVisual.put("ubicacion", coordenadaToMap(evento.getUbicacion()));
                    eventoVisual.put("descripcion", evento.getDescripcion());
                    eventosVisuales.add(eventoVisual);
                }
                rutaVisual.put("eventos", eventosVisuales);

                rutasEstructuradas.add(rutaVisual);
            }
        }

        return rutasEstructuradas;
    }

    /**
     * Convierte una ubicación a un mapa para JSON
     */
    private Map<String, Object> coordenadaToMap(com.glp.glpDP1.domain.Ubicacion ubicacion) {
        if (ubicacion == null) return null;

        Map<String, Object> coord = new HashMap<>();
        coord.put("x", ubicacion.getX());
        coord.put("y", ubicacion.getY());
        return coord;
    }

    /**
     * Estructura la configuración del mapa para visualización
     */
    private Map<String, Object> estructurarConfiguracionMapa(Mapa mapa) {
        Map<String, Object> config = new HashMap<>();

        config.put("ancho", mapa.getAncho());
        config.put("alto", mapa.getAlto());

        // Almacenes
        List<Map<String, Object>> almacenes = new java.util.ArrayList<>();
        for (com.glp.glpDP1.domain.Almacen almacen : mapa.getAlmacenes()) {
            Map<String, Object> almacenVisual = new HashMap<>();
            almacenVisual.put("id", almacen.getId());
            almacenVisual.put("tipo", almacen.getTipo().toString());
            almacenVisual.put("ubicacion", coordenadaToMap(almacen.getUbicacion()));
            almacenVisual.put("capacidadMaxima", almacen.getCapacidadMaxima());
            almacenes.add(almacenVisual);
        }
        config.put("almacenes", almacenes);

        // Bloqueos
        List<Map<String, Object>> bloqueos = new java.util.ArrayList<>();
        for (com.glp.glpDP1.domain.Bloqueo bloqueo : mapa.getBloqueos()) {
            Map<String, Object> bloqueoVisual = new HashMap<>();
            bloqueoVisual.put("id", bloqueo.getId());
            bloqueoVisual.put("horaInicio", bloqueo.getHoraInicio());
            bloqueoVisual.put("horaFin", bloqueo.getHoraFin());

            List<Map<String, Object>> nodos = new java.util.ArrayList<>();
            for (com.glp.glpDP1.domain.Ubicacion nodo : bloqueo.getNodosBloqueados()) {
                nodos.add(coordenadaToMap(nodo));
            }
            bloqueoVisual.put("nodosBloqueados", nodos);
            bloqueos.add(bloqueoVisual);
        }
        config.put("bloqueos", bloqueos);

        return config;
    }

    /**
     * Estructura el resumen de camiones para visualización
     */
    private Map<String, Object> estructurarResumenCamiones(List<Camion> camiones) {
        Map<String, Object> resumen = new HashMap<>();

        List<Map<String, Object>> camionesList = new java.util.ArrayList<>();
        Map<String, Integer> conteoTipos = new HashMap<>();
        Map<String, Integer> conteoEstados = new HashMap<>();

        for (Camion camion : camiones) {
            Map<String, Object> camionVisual = new HashMap<>();
            camionVisual.put("codigo", camion.getCodigo());
            camionVisual.put("tipo", camion.getTipo().toString());
            camionVisual.put("estado", camion.getEstado().toString());
            camionVisual.put("ubicacionActual", coordenadaToMap(camion.getUbicacionActual()));
            camionVisual.put("capacidadTanqueGLP", camion.getCapacidadTanqueGLP());
            camionVisual.put("nivelGLPActual", camion.getNivelGLPActual());
            camionVisual.put("nivelCombustibleActual", camion.getNivelCombustibleActual());

            camionesList.add(camionVisual);

            // Conteos
            String tipo = camion.getTipo().toString();
            String estado = camion.getEstado().toString();
            conteoTipos.put(tipo, conteoTipos.getOrDefault(tipo, 0) + 1);
            conteoEstados.put(estado, conteoEstados.getOrDefault(estado, 0) + 1);
        }

        resumen.put("camiones", camionesList);
        resumen.put("totalCamiones", camiones.size());
        resumen.put("conteoTipos", conteoTipos);
        resumen.put("conteoEstados", conteoEstados);

        return resumen;
    }
}