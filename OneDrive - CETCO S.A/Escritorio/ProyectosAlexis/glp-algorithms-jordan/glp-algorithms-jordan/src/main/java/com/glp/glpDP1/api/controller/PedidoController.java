package com.glp.glpDP1.api.controller;

import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.mapper.PedidoDtoMapper;
import com.glp.glpDP1.services.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Pedido>> obtenerPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@RequestBody PedidoRequest request) {
        Pedido pedido = PedidoDtoMapper.toDomain(request);
        pedidoService.guardarPedido(pedido);
        return ResponseEntity.ok(PedidoDtoMapper.toResponse(pedido));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Pedido>> buscarPorCliente(@PathVariable String idCliente) {
        return ResponseEntity.ok(pedidoService.buscarPorCliente(idCliente));
    }
}
