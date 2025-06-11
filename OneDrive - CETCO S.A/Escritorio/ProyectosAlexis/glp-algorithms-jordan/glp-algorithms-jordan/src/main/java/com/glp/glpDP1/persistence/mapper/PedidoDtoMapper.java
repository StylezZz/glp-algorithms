package com.glp.glpDP1.api.mapper;

import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;
import com.glp.glpDP1.domain.Pedido;

public class PedidoDtoMapper {

    public static Pedido toDomain(PedidoRequest request) {
        return new Pedido(
                request.getIdCliente(),
                null,
                request.getCantidadGLP(),
                request.getHoraRecepcion(),
                4
        );
    }

    public static PedidoResponse toResponse(Pedido pedido) {
        PedidoResponse response = new PedidoResponse();
        response.setId(pedido.getId());
        response.setIdCliente(pedido.getIdCliente());
        response.setCantidadGLP(pedido.getCantidadGLP());
        response.setHoraRecepcion(pedido.getHoraRecepcion());
        response.setHoraEntregaProgramada(pedido.getHoraEntregaProgramada());
        response.setHoraEntregaReal(pedido.getHoraEntregaReal());
        response.setEntregado(pedido.isEntregado());
        response.setCamionAsignado(pedido.getCamionAsignado());
        return response;
    }
}
