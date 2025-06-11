package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.services.impl.AveriaService;
import com.glp.glpDP1.services.impl.AveriasPlanificacionService;
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

@RestController
@RequestMapping("/api/averias-monitoreo")
@RequiredArgsConstructor
@Slf4j
public class AveriasMonitoreoController {

    private final AveriaService averiaService;
    private final AveriasPlanificacionService averiasPlanificacionService;
    
    /**
     * Registra una avería manualmente para un camión
     */
    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrarAveria(
            @RequestParam String codigoCamion,
            @RequestParam String tipoIncidente,
            @RequestParam(required = false) LocalDateTime momento) {
        
        try {
            if (momento == null) {
                momento = LocalDateTime.now();
            }
            
            TipoIncidente tipo;
            try {
                tipo = TipoIncidente.valueOf(tipoIncidente);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de incidente no válido: " + tipoIncidente);
            }
            
            Map<String, Object> resultado = averiasPlanificacionService.registrarAveriaManual(
                codigoCamion, tipo, momento);
            
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.error("Error al registrar avería: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al registrar avería: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al registrar avería", e);
        }
    }
    
    /**
     * Obtiene el historial de averías para monitoreo
     */
    @GetMapping("/historial")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial() {
        try {
            List<Map<String, Object>> historial = averiasPlanificacionService.obtenerHistorialAverias();
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            log.error("Error al obtener historial de averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al obtener historial", e);
        }
    }
    
    /**
     * Obtiene estadísticas de averías
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = averiasPlanificacionService.obtenerEstadisticasAverias();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al obtener estadísticas", e);
        }
    }
    
    /**
     * Genera un informe detallado de averías
     */
    @GetMapping("/informe")
    public ResponseEntity<String> generarInforme() {
        try {
            String informe = averiasPlanificacionService.generarInformeAverias();
            return ResponseEntity.ok(informe);
        } catch (Exception e) {
            log.error("Error al generar informe de averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al generar informe", e);
        }
    }
    
    /**
     * Lista las averías programadas (útil para diagnóstico)
     */
    @GetMapping("/programadas")
    public ResponseEntity<List<Map<String, String>>> listarAveriasProgramadas() {
        try {
            List<Map<String, String>> averiasProgramadas = averiaService.listarAveriasProgramadas();
            return ResponseEntity.ok(averiasProgramadas);
        } catch (Exception e) {
            log.error("Error al listar averías programadas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al listar averías programadas", e);
        }
    }
    
    /**
     * Limpia el historial de averías
     */
    @DeleteMapping("/historial")
    public ResponseEntity<Map<String, Boolean>> limpiarHistorial() {
        try {
            averiasPlanificacionService.limpiarHistorial();
            
            Map<String, Boolean> resultado = new HashMap<>();
            resultado.put("success", true);
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al limpiar historial de averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                            "Error al limpiar historial", e);
        }
    }
}