package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.SimulacionRequest;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulacion")
@RequiredArgsConstructor
@Slf4j
public class SimulacionController {

    // En una implementación real, aquí se inyectaría un SimulacionService
    // private final SimulacionService simulacionService;

    private final Map<String, SimulacionRequest> simulacionesActivas = new HashMap<>();

    /**
     * Inicia una nueva simulación
     */
    @PostMapping("/iniciar")
    public ResponseEntity<String> iniciarSimulacion(@RequestBody SimulacionRequest request) {
        try {
            log.info("Iniciando simulación tipo {}", request.getEscenario());

            // Validar el escenario
            if (request.getEscenario() == null) {
                throw new IllegalArgumentException("El escenario de simulación es obligatorio");
            }

            // Validar fecha de inicio
            if (request.getFechaInicio() == null) {
                request.setFechaInicio(LocalDateTime.now());
            }

            // Generar ID de simulación
            String id = UUID.randomUUID().toString();
            simulacionesActivas.put(id, request);

            // En una implementación real, aquí se llamaría al servicio de simulación

            return ResponseEntity.ok(id);
        } catch (IllegalArgumentException e) {
            log.error("Error al iniciar simulación: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al iniciar simulación: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al iniciar simulación", e);
        }
    }

    /**
     * Detiene una simulación en curso
     */
    @PostMapping("/{id}/detener")
    public ResponseEntity<Boolean> detenerSimulacion(@PathVariable String id) {
        if (!simulacionesActivas.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulación no encontrada");
        }

        // En una implementación real, aquí se detendría la simulación
        simulacionesActivas.remove(id);

        return ResponseEntity.ok(true);
    }

    /**
     * Obtiene el estado actual de una simulación
     */
    @GetMapping("/{id}/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstadoSimulacion(@PathVariable String id) {
        if (!simulacionesActivas.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulación no encontrada");
        }

        // En una implementación real, aquí se consultaría el estado real de la simulación
        SimulacionRequest simulacion = simulacionesActivas.get(id);

        Map<String, Object> estado = new HashMap<>();
        estado.put("id", id);
        estado.put("escenario", simulacion.getEscenario());
        estado.put("fechaInicio", simulacion.getFechaInicio());
        estado.put("activa", true);
        estado.put("progreso", 50); // Ejemplo

        return ResponseEntity.ok(estado);
    }

    /**
     * Lista los escenarios disponibles
     */
    @GetMapping("/escenarios")
    public ResponseEntity<EscenarioSimulacion[]> listarEscenarios() {
        return ResponseEntity.ok(EscenarioSimulacion.values());
    }
}

// Usamos el DTO SimulacionRequest definido en com.glp.glpDP1.api.dto.request