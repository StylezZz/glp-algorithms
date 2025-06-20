package com.glp.glpDP1.repository;

import com.glp.glpDP1.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface PedidoRepository {
    @Query(value = "CALL traerPedidos(:tipoSimulacion, :fecha)", nativeQuery = true)
    List<Object[]> traerPedidos(@Param("tipoSimulacion") Integer tipoSimulacion, @Param("fecha") LocalDate fecha);

    @Query(value = "CALL traerBloqueos(:tipoSimulacion, :fecha)", nativeQuery = true)
    List<Object[]> traerBloqueos(@Param("tipoSimulacion") Integer tipoSimulacion, @Param("fecha") LocalDate fecha);
}
