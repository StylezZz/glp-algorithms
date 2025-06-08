package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.services.FileService;
<<<<<<< HEAD
=======
import com.glp.glpDP1.services.InitService;
import com.glp.glpDP1.services.impl.FileServiceImpl;
>>>>>>> 91bcf2d0a3a64ad22348696dabf167da58f40162
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
            @RequestParam("file") MultipartFile file) {
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
<<<<<<< HEAD
    public ResponseEntity<Map<String,Object>> cargarPedidosHoy(@RequestParam("file") MultipartFile file) {
=======
    public ResponseEntity<Map<String, Object>> cargarPedidosHoy(@RequestParam("file") MultipartFile file) {
>>>>>>> 91bcf2d0a3a64ad22348696dabf167da58f40162
        try {
            log.info("Iniciando algoritmo diario tipo {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            LocalDateTime hoy = LocalDateTime.now();
<<<<<<< HEAD
            List<Pedido> pedidos = fileService.cargarPedidosPorDia(file.getInputStream(), hoy);
=======
            List<Pedido> pedidos = ((FileServiceImpl) fileService).cargarPedidosPorDia(file.getInputStream(), hoy);
>>>>>>> 91bcf2d0a3a64ad22348696dabf167da58f40162

            Map<String, Object> response = new HashMap<>();
            response.put("pedidosCargados", pedidos.size());
            response.put("nombreArchivo", file.getOriginalFilename());
            response.put("fecha", hoy.toLocalDate().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
<<<<<<< HEAD
=======
            log.error("Error inesperado al cargar archivo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        }
    }

    @PostMapping(value = "/pedidos/sem-exp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarPedidosSem(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "fechaInicio", required = false) LocalDateTime fechaInicio,
            @RequestParam(value = "fechaFin", required = false) LocalDateTime fechaFin) { 
        try {
            log.info("Iniciando algoritmo diario tipo {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            LocalDateTime hoy = LocalDateTime.now();
            List<Pedido> pedidos = ((FileServiceImpl) fileService).cargarPedidosSemExp(file.getInputStream(), fechaInicio, fechaFin);

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
     * Nuevo endpoint: Carga pedidos para la semana actual (o la semana especificada)
     */
    @PostMapping(value = "/pedidos/semana", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarPedidosSemana(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) LocalDateTime fechaReferencia) {
        
        try {
            log.info("Cargando pedidos para la semana: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            // Si no se proporciona fecha, usar la actual
            if (fechaReferencia == null) {
                fechaReferencia = LocalDateTime.now();
            }
            
            // Calcular límites de la semana
            LocalDateTime inicioDeSemana = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime finDeSemana = inicioDeSemana.plusDays(6).withHour(23).withMinute(59).withSecond(59);
            
            // Cargar pedidos de la semana
            List<Pedido> pedidosSemana = ((FileServiceImpl) fileService)
                    .cargarPedidosPorSemana(file.getInputStream(), fechaReferencia);

            Map<String, Object> response = new HashMap<>();
            response.put("pedidosCargados", pedidosSemana.size());
            response.put("nombreArchivo", file.getOriginalFilename());
            response.put("fechaInicio", inicioDeSemana.format(DateTimeFormatter.ISO_DATE));
            response.put("fechaFin", finDeSemana.format(DateTimeFormatter.ISO_DATE));
            response.put("idSemana", inicioDeSemana.format(DateTimeFormatter.BASIC_ISO_DATE) + "_" + 
                          finDeSemana.format(DateTimeFormatter.BASIC_ISO_DATE));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inesperado al cargar archivo semanal: {}", e.getMessage(), e);
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
>>>>>>> 91bcf2d0a3a64ad22348696dabf167da58f40162
            log.error("Error inesperado al cargar archivo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        }
    }

    /**
     * Carga un archivo de averías
     */
    @PostMapping(value = "/averias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarAverias(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Cargando archivo de averías: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("El archivo está vacío");
            }

            int averiasRegistradas = fileService.cargarAverias(file.getInputStream());

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
    @DeleteMapping("/averias")
    public ResponseEntity<Map<String, Boolean>> limpiarAverias() {
        try {
            fileService.limpiarAverias();

            Map<String, Boolean> response = new HashMap<>();
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al limpiar averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al limpiar averías", e);
        }
    }

    /**
     * Lista todas las averías programadas
     */
    @GetMapping("/averias")
    public ResponseEntity<List<Map<String, String>>> listarAverias() {
        try {
            List<Map<String, String>> averias = fileService.listarAveriasProgramadas();
            return ResponseEntity.ok(averias);
        } catch (Exception e) {
            log.error("Error al listar averías: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar averías", e);
        }
    }
}