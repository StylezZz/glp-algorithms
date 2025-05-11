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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulacion-semanal")
@RequiredArgsConstructor
@Slf4j
public class SimulacionSemanalController {

    private final SimulacionSemanalService simulacionSemanalService;
    private final DataRepositoryImpl dataRepository;

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
                    fechaInicio
            );

            // Agregar información adicional
            resultado.put("id", UUID.randomUUID().toString());
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
        
        // En una implementación completa, buscaríamos los resultados guardados
        // Para esta demo, devolvemos un error ya que no guardamos resultados
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, 
                                         "Esta funcionalidad requiere persistencia de resultados");
    }

    /**
     * Obtiene estadísticas resumidas de la simulación
     */
    @GetMapping("/estadisticas/{idSimulacion}")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(@PathVariable String idSimulacion) {
        // En una implementación completa, buscaríamos los resultados guardados
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, 
                                         "Esta funcionalidad requiere persistencia de resultados");
    }
}