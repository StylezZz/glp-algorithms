package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.AlgoritmoSimpleRequest;
import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.api.dto.response.AlgoritmoStatusResponse;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import com.glp.glpDP1.services.AlgoritmoService;
import com.glp.glpDP1.services.impl.AlgoritmoServiceImpl;
import com.glp.glpDP1.services.impl.AnalisisPedidosService;
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

    /**
     * Inicia una nueva ejecución del algoritmo de optimización
     * usando los datos ya cargados en el sistema
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
                request.setTamañoPoblacion(250);     // Aumentado de 100
                request.setNumGeneraciones(150);     // Aumentado de 50
                request.setTasaMutacion(0.15);       // Ajustado de 0.05
                request.setTasaCruce(0.85);          // Ajustado de 0.7
                request.setElitismo(15);             // Aumentado de 5
            } else if ("PSO".equalsIgnoreCase(request.getTipoAlgoritmo())) {
                request.setNumParticulas(100);       // Aumentado de 50
                request.setNumIteraciones(200);      // Aumentado de 100
                request.setW(0.6);                   // Ajustado de 0.7
                request.setC1(1.8);                  // Ajustado de 1.5
                request.setC2(1.8);                  // Ajustado de 1.5
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
            String id = ((AlgoritmoServiceImpl)algoritmoService).iniciarAlgoritmoDiario(request);
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
            String id = ((AlgoritmoServiceImpl)algoritmoService).iniciarAlgoritmoGeneticoDiario(request);
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
                    entregasTotales > 0 ? (double)entregasATiempo / entregasTotales * 100 : 0);

            return ResponseEntity.ok(datosSimulacion);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener resultados de simulación", e);
        }
    }
}