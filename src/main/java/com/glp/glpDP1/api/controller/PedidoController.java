package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;
import com.glp.glpDP1.services.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoController {

    private final PedidoService pedidoService;

    // GET: Lista todos los pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> obtenerPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    // POST: Crea un nuevo pedido
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@RequestBody PedidoRequest request) {
        PedidoResponse creado = pedidoService.guardarPedido(request);
        return ResponseEntity.ok(creado);
    }

    // GET: Filtra pedidos por cliente
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<PedidoResponse>> buscarPorCliente(@PathVariable String idCliente) {
        return ResponseEntity.ok(pedidoService.buscarPorCliente(idCliente));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPedidos(@RequestParam("file") MultipartFile file) {
        try {
            pedidoService.procesarArchivoPedidos(file);
            return ResponseEntity.ok("Archivo procesado correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al procesar archivo: " + e.getMessage());
        }
    }


}
