package com.glp.glpDP1.api.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.glp.glpDP1.services.SimulationStateService;
import com.glp.glpDP1.api.dto.InicializarSimulacionRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final SimulationStateService simulationStateService;

    /**
     * Inicializa la simulación con los resultados de un algoritmo
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> inicializarSimulacion(
            @RequestBody InicializarSimulacionRequest request
    ) {
        try {
            simulationStateService.inicializarSimulacion(request.getAlgoritmoId(), request.getFechaInicial());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "mensaje", "Simulación inicializada correctamente",
                    "algoritmoId", request.getAlgoritmoId(),
                    "wsEndpoint", "/ws/simulation",
                    "fecha", request.getFechaInicial()
            ));
        } catch (Exception e) {
            log.error("Error al inicializar simulación: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "mensaje", "Error al inicializar simulación: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el estado actual de la simulación
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> obtenerEstadoSimulacion() {
        return ResponseEntity.ok(Map.of(
                "activa", simulationStateService.isSimulacionActiva(),
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Finaliza la simulación actual
     */
    @PostMapping("/finalize")
    public ResponseEntity<Map<String, Object>> finalizarSimulacion() {
        simulationStateService.finalizarSimulacion();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "mensaje", "Simulación finalizada"
        ));
    }
}