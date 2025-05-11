package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.AlgoritmoSimpleRequest;
import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.api.dto.response.AlgoritmoStatusResponse;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.SimuladorEntregas;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import com.glp.glpDP1.services.AlgoritmoService;
import com.glp.glpDP1.services.impl.AlgoritmoServiceImpl;
import com.glp.glpDP1.services.impl.AnalisisPedidosService;
import com.glp.glpDP1.services.impl.AveriaService;
import com.glp.glpDP1.services.impl.OptimizacionMultipleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
@Slf4j
public class AlgoritmoController {

    @Autowired
    private final AlgoritmoService algoritmoService;

    @Autowired
    private OptimizacionMultipleService optimizacionMultipleService;
    @Autowired
    private DataRepositoryImpl dataRepositoryImpl;

    @Autowired
    private AveriaService averiaService;

    @PostMapping("/start-multiple")
    public ResponseEntity<Map<String, Object>> iniciarMultiple(
            @RequestBody AlgoritmoSimpleRequest request,
            @RequestParam(defaultValue = "10") int numEjecuciones) {

        try {
            // Obtener datos del repositorio
            List<Camion> camiones = dataRepositoryImpl.obtenerCamiones();
            List<Pedido> pedidos = dataRepositoryImpl.obtenerPedidos();
            Mapa mapa = dataRepositoryImpl.obtenerMapa();

            // Si no se especificó momento actual, usar el actual
            LocalDateTime momentoActual = request.getMomentoActual() != null ? request.getMomentoActual()
                    : LocalDateTime.now();

            // Convertir escenario
            EscenarioSimulacion escenario = null;
            if (request.getEscenario() != null) {
                try {
                    escenario = EscenarioSimulacion.valueOf(request.getEscenario());
                } catch (IllegalArgumentException e) {
                    escenario = EscenarioSimulacion.DIA_A_DIA;
                }
            } else {
                escenario = EscenarioSimulacion.DIA_A_DIA;
            }

            // Ejecutar algoritmo múltiples veces
            OptimizacionMultipleService.ResultadoOptimizacion resultado;

            if ("GENETICO".equalsIgnoreCase(request.getTipoAlgoritmo())) {
                resultado = optimizacionMultipleService.ejecutarMultipleGenetico(
                        camiones, pedidos, mapa, momentoActual, escenario, numEjecuciones);
            } else {
                resultado = optimizacionMultipleService.ejecutarMultiplePSO(
                        camiones, pedidos, mapa, momentoActual, escenario, numEjecuciones);
            }

            // Iniciar algoritmo con la mejor solución
            String id = UUID.randomUUID().toString();

            // Crear objeto de estado y resultado
            AlgoritmoStatusResponse estado = new AlgoritmoStatusResponse(
                    id,
                    AlgoritmoStatusResponse.EstadoAlgoritmo.COMPLETADO,
                    100.0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    resultado.fitness);

            // Calcular métricas
            double distanciaTotal = resultado.rutas.stream()
                    .mapToDouble(Ruta::getDistanciaTotal).sum();

            double consumoCombustible = resultado.rutas.stream()
                    .mapToDouble(Ruta::getConsumoCombustible).sum();

            // Crear resultado y almacenar
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", id);
            respuesta.put("rutas", resultado.rutas);
            respuesta.put("fitness", resultado.fitness);
            respuesta.put("numEjecuciones", numEjecuciones);
            respuesta.put("pedidosAsignados", resultado.pedidosAsignados);
            respuesta.put("pedidosTotales", pedidos.size());
            respuesta.put("distanciaTotal", distanciaTotal);
            respuesta.put("consumoCombustible", consumoCombustible);
            respuesta.put("tiempoEjecucionMs", resultado.tiempoEjecucionMs);

            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("Error al ejecutar optimización múltiple: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al ejecutar optimización", e);
        }
    }

    /**
     * Inicia una nueva ejecución del algoritmo de optimización
     * usando los datos ya cargados en el sistema
     * 
     * @param request Parámetros de ejecución
     * @return ID de la ejecución
     */
    @PostMapping("/start")
    public ResponseEntity<String> iniciarAlgoritmo(@RequestBody AlgoritmoSimpleRequest request) {
        try {
            log.info("Iniciando algoritmo tipo {}", request.getTipoAlgoritmo());

            // Iniciar algoritmo con datos ya cargados
            String id = algoritmoService.iniciarAlgoritmo(request);
            return ResponseEntity.ok(id);
        } catch (IllegalArgumentException e) {
            log.error("Error al iniciar algoritmo: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al iniciar algoritmo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar el algoritmo", e);
        }
    }

    /**
     * Consulta el estado actual de la ejecución de un algoritmo
     * 
     * @param id ID de la ejecución
     * @return Estado actual
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<AlgoritmoStatusResponse> consultarEstado(@PathVariable String id) {
        try {
            AlgoritmoStatusResponse estado = algoritmoService.consultarEstado(id);
            return ResponseEntity.ok(estado);
        } catch (NoSuchElementException e) {
            log.error("No se encontró la ejecución: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al consultar estado de algoritmo {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar el estado", e);
        }
    }

    /**
     * Obtiene los resultados de una ejecución completada
     * 
     * @param id ID de la ejecución
     * @return Resultados de la optimización
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<AlgoritmoResultResponse> obtenerResultados(@PathVariable String id) {
        try {
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(id);
            return ResponseEntity.ok(resultado);
        } catch (NoSuchElementException e) {
            log.error("No se encontraron resultados para la ejecución: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("Estado inválido para obtener resultados de {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al obtener resultados del algoritmo {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener los resultados", e);
        }
    }

    /**
     * Cancela una ejecución en curso
     * 
     * @param id ID de la ejecución
     * @return Resultado de la cancelación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> cancelarEjecucion(@PathVariable String id) {
        try {
            boolean resultado = algoritmoService.cancelarEjecucion(id);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al cancelar ejecución {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cancelar la ejecución", e);
        }
    }

    // En AlgoritmoController.java - Parámetros optimizados
    @PostMapping("/start-optimized")
    public ResponseEntity<String> iniciarAlgoritmoOptimizado(@RequestBody AlgoritmoSimpleRequest request) {
        try {
            // Establecer parámetros optimizados
            if ("GENETICO".equalsIgnoreCase(request.getTipoAlgoritmo())) {
                request.setTamañoPoblacion(250); // Aumentado de 100
                request.setNumGeneraciones(150); // Aumentado de 50
                request.setTasaMutacion(0.15); // Ajustado de 0.05
                request.setTasaCruce(0.85); // Ajustado de 0.7
                request.setElitismo(15); // Aumentado de 5
            } else if ("PSO".equalsIgnoreCase(request.getTipoAlgoritmo())) {
                request.setNumParticulas(100); // Aumentado de 50
                request.setNumIteraciones(200); // Aumentado de 100
                request.setW(0.6); // Ajustado de 0.7
                request.setC1(1.8); // Ajustado de 1.5
                request.setC2(1.8); // Ajustado de 1.5
            }

            // Iniciar algoritmo con parámetros optimizados
            String id = algoritmoService.iniciarAlgoritmo(request);
            return ResponseEntity.ok(id);
        } catch (Exception e) {
            log.error("Error al iniciar algoritmo optimizado: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar algoritmo", e);
        }
    }

    @Autowired
    private AnalisisPedidosService analisisPedidosService;

    @GetMapping("/analisis/{id}")
    public ResponseEntity<Map<String, Object>> analizarResultado(@PathVariable String id) {
        try {
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(id);
            List<Pedido> todosPedidos = dataRepositoryImpl.obtenerPedidos();

            Map<String, Object> analisis = analisisPedidosService.analizarPedidosNoAsignados(
                    todosPedidos, resultado.getRutas());

            return ResponseEntity.ok(analisis);
        } catch (Exception e) {
            log.error("Error al analizar resultado: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al analizar resultado", e);
        }
    }

    @GetMapping("/analisis/{id}/informe")
    public ResponseEntity<String> obtenerInformeAnalisis(@PathVariable String id) {
        try {
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(id);
            List<Pedido> todosPedidos = dataRepositoryImpl.obtenerPedidos();

            String informe = analisisPedidosService.generarInformeNoAsignados(
                    todosPedidos, resultado.getRutas());

            return ResponseEntity.ok(informe);
        } catch (Exception e) {
            log.error("Error al generar informe: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar informe", e);
        }
    }

    @PostMapping("/start-diario")
    public ResponseEntity<String> iniciarAlgoritmoDiario(@RequestBody AlgoritmoSimpleRequest request) {
        try {
            log.info("Iniciando algoritmo diario tipo {}", request.getTipoAlgoritmo());
            String id = ((AlgoritmoServiceImpl) algoritmoService).iniciarAlgoritmoDiario(request);
            return ResponseEntity.ok(id);
        } catch (IllegalStateException e) {
            log.warn("No hay pedidos para hoy: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al iniciar algoritmo diario: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar algoritmo", e);
        }
    }

    @PostMapping("/genetico/start-diario")
    public ResponseEntity<String> iniciarAlgoritmoGeneticoDiario(@RequestBody AlgoritmoSimpleRequest request) {
        try {
            log.info("Iniciando algoritmo genético diario");
            String id = ((AlgoritmoServiceImpl) algoritmoService).iniciarAlgoritmoGeneticoDiario(request);
            return ResponseEntity.ok(id);
        } catch (IllegalStateException e) {
            log.warn("No hay pedidos para hoy: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al iniciar algoritmo genético diario: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar algoritmo", e);
        }
    }

    @GetMapping("/simulacion/{id}")
    public ResponseEntity<Map<String, Object>> obtenerResultadosSimulacion(@PathVariable String id) {
        try {
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(id);

            // Extraer información específica de simulación
            Map<String, Object> datosSimulacion = new HashMap<>();
            datosSimulacion.put("rutas", resultado.getRutas());

            // Estadísticas de entregas
            int entregasTotales = 0;
            int entregasATiempo = 0;
            int entregasRetrasadas = 0;

            for (Ruta ruta : resultado.getRutas()) {
                for (Pedido pedido : ruta.getPedidosAsignados()) {
                    if (pedido.isEntregado()) {
                        entregasTotales++;

                        // Verificar si la entrega fue a tiempo
                        if (pedido.getHoraEntregaReal().isBefore(pedido.getHoraLimiteEntrega()) ||
                                pedido.getHoraEntregaReal().isEqual(pedido.getHoraLimiteEntrega())) {
                            entregasATiempo++;
                        } else {
                            entregasRetrasadas++;
                        }
                    }
                }
            }

            datosSimulacion.put("entregasTotales", entregasTotales);
            datosSimulacion.put("entregasATiempo", entregasATiempo);
            datosSimulacion.put("entregasRetrasadas", entregasRetrasadas);
            datosSimulacion.put("porcentajeCumplimiento",
                    entregasTotales > 0 ? (double) entregasATiempo / entregasTotales * 100 : 0);

            return ResponseEntity.ok(datosSimulacion);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener resultados de simulación", e);
        }
    }

    // Añadir este método en la clase AlgoritmoController
    @PostMapping("/simulate-with-failures/{id}")
    public ResponseEntity<Map<String, Object>> simularConAverias(@PathVariable String id) {
        try {
            AlgoritmoResultResponse resultado = algoritmoService.obtenerResultados(id);
            List<Camion> camiones = dataRepositoryImpl.obtenerCamiones();
            Mapa mapa = dataRepositoryImpl.obtenerMapa();

            // Crear simulador con servicio de averías
            SimuladorEntregas simulador = new SimuladorEntregas(averiaService);

            // Simular con averías
            List<Ruta> rutasSimuladas = simulador.simularEntregas(
                    resultado.getRutas(),
                    LocalDateTime.now(),
                    mapa,
                    true // considerar averías
            );

            // Calcular métricas
            int totalPedidos = resultado.getPedidosTotales();
            int entregados = 0;
            int cancelados = 0;

            for (Ruta ruta : rutasSimuladas) {
                if (ruta.isCompletada()) {
                    entregados += ruta.getPedidosAsignados().size();
                } else if (ruta.isCancelada()) {
                    cancelados++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("rutasSimuladas", rutasSimuladas);
            response.put("pedidosEntregados", entregados);
            response.put("pedidosTotales", totalPedidos);
            response.put("rutasCanceladas", cancelados);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al simular con averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error en simulación con averías", e);
        }
    }
}