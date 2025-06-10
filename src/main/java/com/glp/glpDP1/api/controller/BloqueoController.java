package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.services.BloqueoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bloqueos") // Ruta base
@RequiredArgsConstructor
public class BloqueoController {

    private final BloqueoService bloqueoService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadBloqueos(@RequestParam("file") MultipartFile file) {
        try {
            bloqueoService.procesarArchivoBloqueos(file);
            return ResponseEntity.ok("Archivo de bloqueos procesado correctamente.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar bloqueos: " + e.getMessage());
        }
    }
}
