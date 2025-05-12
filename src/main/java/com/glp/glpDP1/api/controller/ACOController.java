package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.ACORequest;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import com.glp.glpDP1.services.ACOService;
import com.glp.glpDP1.services.impl.AnalisisPedidosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Controlador para el algoritmo ACO
 */
@RestController
@RequestMapping("/api/aco")
@RequiredArgsConstructor
@Slf4j
public class ACOController {

    @Autowired
    private final ACOService acoService;

    @Autowired
    private DataRepositoryImpl dataRepositoryImpl;

    @Autowired
    private AnalisisPedidosService analisisPedidosService;

    /**
     * Inicia una ejecución del algoritmo ACO con parámetros personalizados
     */
    @PostMapping("/start")
    public ResponseEntity<String> iniciarACO(@RequestBody ACORequest request) {
        try {
            log.info("Iniciando algoritmo ACO");

            // Obtener datos del repositorio
            List<Camion> camiones = dataRepositoryImpl.obtenerCamiones();
            List<Pedido> pedidos = dataRepositoryImpl.obtenerPedidos();
            Mapa mapa = dataRepositoryImpl.obtenerMapa();

            // Si no se especificó momento actual, usar el actual
            if (request.getMomentoActual() == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Argumentos incorrectos");

            LocalDateTime momentoActual = request.getMomentoActual();

            // Convertir escenario
            EscenarioSimulacion escenario;
            if (request.getEscenario() != null) {
                try {
                    escenario = EscenarioSimulacion.valueOf(request.getEscenario());
                } catch (IllegalArgumentException e) {
                    escenario = EscenarioSimulacion.DIA_A_DIA;
                }
            } else {
                escenario = EscenarioSimulacion.DIA_A_DIA;
            }

            // Iniciar algoritmo ACO
            String id = acoService.ejecutarACO(
                    camiones,
                    pedidos,
                    mapa,
                    momentoActual,
                    escenario,
                    request.getNumHormigas(),
                    request.getNumIteraciones(),
                    request.getAlfa(),
                    request.getBeta(),
                    request.getRho(),
                    request.getQ0()
            );

            return ResponseEntity.ok(id);
        } catch (Exception e) {
            log.error("Error al iniciar algoritmo ACO: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar el algoritmo", e);
        }
    }

    /**
     * Consulta el estado actual de la ejecución de un algoritmo
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> consultarEstado(@PathVariable String id) {
        try {
            Map<String, Object> estado = acoService.obtenerEstado(id);
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
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<Map<String, Object>> obtenerResultados(@PathVariable String id) {
        try {
            Map<String, Object> resultado = acoService.obtenerResultados(id);
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
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> cancelarEjecucion(@PathVariable String id) {
        try {
            boolean resultado = acoService.cancelarEjecucion(id);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al cancelar ejecución {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cancelar la ejecución", e);
        }
    }

    /**
     * Analiza los resultados de una ejecución
     */
    @GetMapping("/analisis/{id}")
    public ResponseEntity<Map<String, Object>> analizarResultado(@PathVariable String id) {
        try {
            Map<String, Object> resultado = acoService.obtenerResultados(id);
            List<Pedido> todosPedidos = dataRepositoryImpl.obtenerPedidos();

            @SuppressWarnings("unchecked")
            List<com.glp.glpDP1.domain.Ruta> rutas = (List<com.glp.glpDP1.domain.Ruta>) resultado.get("rutas");

            Map<String, Object> analisis = analisisPedidosService.analizarPedidosNoAsignados(
                    todosPedidos, rutas);

            return ResponseEntity.ok(analisis);
        } catch (Exception e) {
            log.error("Error al analizar resultado: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al analizar resultado", e);
        }
    }

    /**
     * Genera un informe detallado de los pedidos no asignados
     */
    @GetMapping("/analisis/{id}/informe")
    public ResponseEntity<String> obtenerInformeAnalisis(@PathVariable String id) {
        try {
            Map<String, Object> resultado = acoService.obtenerResultados(id);
            List<Pedido> todosPedidos = dataRepositoryImpl.obtenerPedidos();

            @SuppressWarnings("unchecked")
            List<com.glp.glpDP1.domain.Ruta> rutas = (List<com.glp.glpDP1.domain.Ruta>) resultado.get("rutas");

            String informe = analisisPedidosService.generarInformeNoAsignados(
                    todosPedidos, rutas);

            return ResponseEntity.ok(informe);
        } catch (Exception e) {
            log.error("Error al generar informe: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar informe", e);
        }
    }

    /**
     * Compara los resultados del algoritmo ACO con otros algoritmos
     */
    @GetMapping("/comparar/{idACO}/{idOtro}")
    public ResponseEntity<Map<String, Object>> compararResultados(
            @PathVariable String idACO,
            @PathVariable String idOtro,
            @RequestParam(defaultValue = "GENETICO") String algoritmoComparacion) {

        try {
            // Obtener resultados de ACO
            Map<String, Object> resultadoACO = acoService.obtenerResultados(idACO);

            // Obtener resultados del otro algoritmo mediante el AlgoritmoService estándar
            // Usando polimorfismo para manejar los diferentes tipos de respuesta
            Map<String, Object> comparacion = new java.util.HashMap<>();

            comparacion.put("algoritmo1", "ACO");
            comparacion.put("algoritmo2", algoritmoComparacion);
            comparacion.put("resultadoACO", resultadoACO);

            // Aquí iría la lógica para obtener y comparar con el otro algoritmo
            // Por simplicidad, solo devolvemos los resultados del ACO
            // En una implementación completa, habría que integrarlo con AlgoritmoService

            return ResponseEntity.ok(comparacion);
        } catch (Exception e) {
            log.error("Error al comparar resultados: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comparar resultados", e);
        }
    }
}