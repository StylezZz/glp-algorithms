package com.glp.glpDP1.mapper;

import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class PedidoDtoMapper {

    public static Pedido toDomain(PedidoRequest dto) {
        return new Pedido(
                dto.getId(),
                dto.getIdCliente(),
                new Ubicacion(dto.getX(), dto.getY()),
                dto.getCantidadGLP(),
                LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.parse(dto.getHoraRecepcion())),
                Integer.parseInt(dto.getTiempoLimiteEntrega()) // si es un número en string
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
