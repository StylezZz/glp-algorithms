package com.glp.glpDP1.repository;

import com.glp.glpDP1.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {
    @Query(value = "SELECT * FROM traerPedidos(:tipoSimulacion, :fecha)", nativeQuery = true)
    List<Object[]> traerPedidos(@Param("tipoSimulacion") Integer tipoSimulacion, @Param("fecha") LocalDate fecha);

    @Query(value = "SELECT * FROM traerBloqueos(:tipoSimulacion, :fecha)", nativeQuery = true)
    List<Object[]> traerBloqueos(@Param("tipoSimulacion") Integer tipoSimulacion, @Param("fecha") LocalDate fecha);

    List<PedidoEntity> findByIdCliente(String idCliente);
}
