package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.AlgoritmoSimpleRequest;
import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.api.dto.response.AlgoritmoStatusResponse;
import com.glp.glpDP1.services.AlgoritmoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
@Slf4j
public class AlgoritmoController {

    @Autowired
    private final AlgoritmoService algoritmoService;

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
}