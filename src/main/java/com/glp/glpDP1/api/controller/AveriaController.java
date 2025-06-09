package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.services.impl.AveriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para gestión de averías
 */
@RestController
@RequestMapping("/api/averias")
@RequiredArgsConstructor
@Slf4j
public class AveriaController {

    private final AveriaService averiaService;

    /**
     * Carga un archivo de averías
     */
    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarAverias(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Cargando archivo de averías: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            int averiasRegistradas = averiaService.cargarAverias(file.getInputStream());

            Map<String, Object> response = new HashMap<>();
            response.put("averiasRegistradas", averiasRegistradas);
            response.put("nombreArchivo", file.getOriginalFilename());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error en los datos del archivo: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error al leer el archivo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo", e);
        } catch (Exception e) {
            log.error("Error inesperado al cargar archivo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        }
    }

    /**
     * Limpia todas las averías registradas
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Boolean>> limpiarAverias() {
        try {
            averiaService.limpiarAverias();
            
            Map<String, Boolean> response = new HashMap<>();
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al limpiar averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al limpiar averías", e);
        }
    }
}