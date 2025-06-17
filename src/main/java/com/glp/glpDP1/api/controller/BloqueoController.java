package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.services.BloqueoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint para cargar el archivo de bloqueos.
 *
 *  ▸ POST /api/bloqueos/upload  (form-data : file = <archivo .txt>)
 */
@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
@Slf4j
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadBloqueos(@RequestParam("file") MultipartFile file) {

        try {
            /* 1️⃣  Limpia la tabla */
            bloqueoService.limpiarBloqueos();

            /* 2️⃣  Carga y devuelve cuántas filas se insertaron */
            int total = bloqueoService.cargarBloqueos(file);
            return ResponseEntity.ok("✅  Se cargaron " + total + " bloqueos.");

        } catch (Exception ex) {
            log.error("Error al procesar bloqueos", ex);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("❌  Error al procesar bloqueos: " + ex.getMessage());
        }
    }
}
