package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.persistence.entity.PedidoEntity;
import com.glp.glpDP1.persistence.mapper.PedidoMapper;
import com.glp.glpDP1.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public void guardarPedido(Pedido pedido) {
        PedidoEntity entity = PedidoMapper.toEntity(pedido);
        pedidoRepository.save(entity);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoMapper::toDomain)
                .collect(Collectors.toList());
    }

    public List<Pedido> buscarPorCliente(String idCliente) {
        return pedidoRepository.findByIdCliente(idCliente)
                .stream()
                .map(PedidoMapper::toDomain)
                .collect(Collectors.toList());
    }
}
