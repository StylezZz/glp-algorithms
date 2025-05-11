package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.services.FileService;
import com.glp.glpDP1.services.InitService;
import com.glp.glpDP1.services.impl.FileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;
    private final InitService initService;

    /**
     * Carga un archivo de pedidos (ventas)
     */
    @PostMapping(value = "/pedidos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarPedidos(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Cargando archivo de pedidos: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            List<Pedido> pedidos = fileService.cargarPedidos(file.getInputStream());

            Map<String, Object> response = new HashMap<>();
            response.put("pedidosCargados", pedidos.size());
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
     * Carga un archivo de bloqueos
     */
    @PostMapping(value = "/bloqueos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarBloqueos(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapa", required = false) String mapaId) {
        try {
            log.info("Cargando archivo de bloqueos: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            List<Bloqueo> bloqueos = fileService.cargarBloqueos(file.getInputStream());

            // En una implementación real, aquí se asociarían los bloqueos al mapa
            // usando un servicio adicional si mapaId está definido

            Map<String, Object> response = new HashMap<>();
            response.put("bloqueosCargados", bloqueos.size());
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

    @PostMapping(value = "/pedidos/hoy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarPedidosHoy(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Iniciando algoritmo diario tipo {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            LocalDateTime hoy = LocalDateTime.now();
            List<Pedido> pedidos = ((FileServiceImpl) fileService).cargarPedidosPorDia(file.getInputStream(), hoy);

            Map<String, Object> response = new HashMap<>();
            response.put("pedidosCargados", pedidos.size());
            response.put("nombreArchivo", file.getOriginalFilename());
            response.put("fecha", hoy.toLocalDate().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inesperado al cargar archivo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        }
    }

    /**
     * Carga un archivo de mantenimiento preventivo
     */
    @PostMapping(value = "/mantenimiento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarMantenimiento(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Cargando archivo de mantenimiento: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            int registrosCargados = initService.cargarPlanMantenimiento(file.getInputStream());

            Map<String, Object> response = new HashMap<>();
            response.put("registrosCargados", registrosCargados);
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
}