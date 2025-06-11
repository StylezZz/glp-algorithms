package com.glp.glpDP1.persistence.mapper;

import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.persistence.entity.PedidoEntity;

public class PedidoMapper {

    public static PedidoEntity toEntity(Pedido pedido) {
        PedidoEntity entity = new PedidoEntity();
        entity.setIdCliente(pedido.getIdCliente());
        entity.setCantidadGLP(pedido.getCantidadGLP());
        entity.setHoraRecepcion(pedido.getHoraRecepcion());
        entity.setHoraEntregaProgramada(pedido.getHoraEntregaProgramada());
        entity.setHoraEntregaReal(pedido.getHoraEntregaReal());
        entity.setCamionAsignado(pedido.getCamionAsignado());
        entity.setEntregado(pedido.isEntregado());
        return entity;
    }

    public static Pedido toDomain(PedidoEntity entity) {
        Pedido pedido = new Pedido(
                entity.getIdCliente(),
                null, // Ubicación no está en la entidad todavía
                entity.getCantidadGLP(),
                entity.getHoraRecepcion(),
                4 // Asumimos 4 horas como límite por defecto (puedes ajustar)
        );
        pedido.setHoraEntregaProgramada(entity.getHoraEntregaProgramada());
        pedido.setHoraEntregaReal(entity.getHoraEntregaReal());
        pedido.setCamionAsignado(entity.getCamionAsignado());
        return pedido;
    }
}
