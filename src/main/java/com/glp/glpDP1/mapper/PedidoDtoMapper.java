package com.glp.glpDP1.mapper;

import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;
import com.glp.glpDP1.persistence.entity.PedidoEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class PedidoDtoMapper {

    // De DTO a Dominio
    public static Pedido toDomain(PedidoRequest dto) {
        return new Pedido(
                dto.getId(),
                dto.getIdCliente(),
                new Ubicacion(dto.getX(), dto.getY()),
                dto.getCantidadGLP(),
                LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.parse(dto.getHoraRecepcion())),
                Integer.parseInt(dto.getTiempoLimiteEntrega())
        );
    }

    // De Dominio a DTO de respuesta
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

    // De Entidad a Dominio
    public static Pedido toDomain(PedidoEntity entity) {
        Pedido pedido = new Pedido(
                entity.getId(),
                entity.getIdCliente(),
                new Ubicacion(entity.getX(), entity.getY()),
                entity.getCantidadGLP(),
                entity.getHoraRecepcion(),
                entity.getTiempoLimiteEntrega()
        );
        pedido.setHoraEntregaProgramada(entity.getHoraEntregaProgramada());
        pedido.setHoraEntregaReal(entity.getHoraEntregaReal());
        pedido.setCamionAsignado(entity.getCamionAsignado());
        return pedido;
    }

    public static PedidoEntity toEntity(Pedido pedido) {
        System.out.println("[DEBUG] Mapeando Pedido a Entity con x=" + pedido.getUbicacion().getX() + ", y=" + pedido.getUbicacion().getY());

        PedidoEntity entity = new PedidoEntity();
        entity.setIdCliente(pedido.getIdCliente());
        entity.setCantidadGLP(pedido.getCantidadGLP());
        entity.setHoraRecepcion(pedido.getHoraRecepcion());
        entity.setTiempoLimiteEntrega((int) pedido.getTiempoLimiteEntrega().toHours());
        entity.setHoraEntregaProgramada(pedido.getHoraEntregaProgramada());
        entity.setHoraEntregaReal(pedido.getHoraEntregaReal());
        entity.setEntregado(pedido.isEntregado());
        entity.setCamionAsignado(pedido.getCamionAsignado());

        // Asegura que se estén seteando bien los valores
        entity.setX(pedido.getUbicacion().getX());
        entity.setY(pedido.getUbicacion().getY());

        return entity;
    }


}
