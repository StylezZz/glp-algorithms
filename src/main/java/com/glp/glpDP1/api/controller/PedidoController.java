package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.domain.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoController {

    // En una implementación real, aquí se inyectaría un PedidoService
    // private final PedidoService pedidoService;

    // Para simplificar, usamos una lista en memoria
    private final List<Pedido> pedidos = new ArrayList<>();

    /**
     * Obtiene todos los pedidos pendientes
     */
    @GetMapping
    public ResponseEntity<List<Pedido>> obtenerPedidos() {
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Crea un nuevo pedido
     */
    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@RequestBody Pedido pedido) {
        try {
            // En una implementación real, aquí se guardaría en la base de datos
            pedidos.add(pedido);
            return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
        } catch (Exception e) {
            log.error("Error al crear pedido: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear el pedido", e);
        }
    }

    /**
     * Obtiene un pedido por su ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable String id) {
        return pedidos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    /**
     * Elimina un pedido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable String id) {
        boolean removed = pedidos.removeIf(p -> p.getId().equals(id));
        if (removed) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        }
    }
}