package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.BloqueoRequest;
import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.services.MapaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mapa")
@RequiredArgsConstructor
@Slf4j
public class MapaController {

    private final MapaService mapaService;

    /**
     * Obtiene el mapa actual con su configuración
     */
    @GetMapping
    public ResponseEntity<Mapa> obtenerMapa() {
        try {
            Mapa mapa = mapaService.obtenerMapa();
            return ResponseEntity.ok(mapa);
        } catch (Exception e) {
            log.error("Error al obtener mapa: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener el mapa", e);
        }
    }

    /**
     * Obtiene los bloqueos actuales del mapa
     */
    @GetMapping("/bloqueos")
    public ResponseEntity<List<Bloqueo>> obtenerBloqueos() {
        try {
            Mapa mapa = mapaService.obtenerMapa();
            return ResponseEntity.ok(mapa.getBloqueos());
        } catch (Exception e) {
            log.error("Error al obtener bloqueos: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener los bloqueos", e);
        }
    }

    /**
     * Agrega un nuevo bloqueo al mapa
     */
    @PostMapping("/bloqueos")
    public ResponseEntity<Bloqueo> agregarBloqueo(@RequestBody BloqueoRequest request) {
        try {
            log.info("Agregando nuevo bloqueo: {} - {}", request.getHoraInicio(), request.getHoraFin());

            if (request.getNodos() == null || request.getNodos().isEmpty()) {
                throw new IllegalArgumentException("Debe proporcionar al menos un nodo para el bloqueo");
            }

            Bloqueo bloqueo = new Bloqueo(request.getHoraInicio(), request.getHoraFin(), request.getNodos());
            mapaService.agregarBloqueo(bloqueo);

            return ResponseEntity.status(HttpStatus.CREATED).body(bloqueo);
        } catch (IllegalArgumentException e) {
            log.error("Error en los datos del bloqueo: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al agregar bloqueo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al agregar el bloqueo", e);
        }
    }

    /**
     * Verifica si una ubicación está bloqueada en un momento dado
     */
    @GetMapping("/bloqueos/verificar")
    public ResponseEntity<Boolean> verificarBloqueo(
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam(required = false) LocalDateTime momento) {
        try {
            if (momento == null) {
                momento = LocalDateTime.now();
            }

            Mapa mapa = mapaService.obtenerMapa();
            Ubicacion ubicacion = new Ubicacion(x, y);
            boolean bloqueado = mapa.estaBloqueado(ubicacion, momento);

            return ResponseEntity.ok(bloqueado);
        } catch (Exception e) {
            log.error("Error al verificar bloqueo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al verificar el bloqueo", e);
        }
    }
}

// Usamos BloqueoRequest de com.glp.glpDP1.api.dto.request