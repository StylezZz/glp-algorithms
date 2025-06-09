package com.glp.glpDP1.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String idCliente;
    private double cantidadGLP;
    private LocalDateTime horaRecepcion;
    private LocalDateTime horaEntregaProgramada;
    private LocalDateTime horaEntregaReal;
    private boolean entregado;
    private String camionAsignado;

}
