package com.glp.glpDP1.repository;

import com.glp.glpDP1.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<PedidoEntity, String> {
    List<PedidoEntity> findByIdCliente(String idCliente);
}
