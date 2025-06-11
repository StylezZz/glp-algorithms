package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.services.impl.MonitoreoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoreo")
@RequiredArgsConstructor
@Slf4j
public class MonitoreoController {
    private final MonitoreoService monitoreoService;

    @GetMapping("/rutas")
    public ResponseEntity<Map<String,MonitoreoService.EstadoRuta>>obtenerEstadoRutas(){
        return ResponseEntity.ok(monitoreoService.obtenerEstadoRutas());
    }

    @GetMapping("/bloqueos")
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosActivos(){
        return ResponseEntity.ok(monitoreoService.obtenerBloqueosActivos(LocalDateTime.now()));
    }

    @GetMapping("/bloqueos/{timestamp}")
    public ResponseEntity<List<Bloqueo>> obtenerBloqueosEnMomento(@PathVariable LocalDateTime timestamp) {
        return ResponseEntity.ok(monitoreoService.obtenerBloqueosActivos(timestamp));
    }
}
