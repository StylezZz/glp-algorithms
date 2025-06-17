package com.glp.glpDP1.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedido") // coincide con el nombre exacto en tu BD
@Getter
@Setter
@NoArgsConstructor
public class PedidoEntity {

    @Id
    @Column(name = "id_pedido")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "id_cliente")
    private String idCliente;

    @Column(name = "cantidad_glp")
    private double cantidadGLP;

    @Column(name = "hora_recepcion")
    private LocalDateTime horaRecepcion;

    @Column(name = "tiempo_limite_entrega")
    private int tiempoLimiteEntrega; // si lo tratas como minutos

    @Column(name = "hora_entrega_programada")
    private LocalDateTime horaEntregaProgramada;

    @Column(name = "hora_entrega_real")
    private LocalDateTime horaEntregaReal;

    @Column(name = "camion_asignado")
    private String camionAsignado;

    @Column(name = "entregado")
    private boolean entregado;

    @Column(name = "x")
    private int x;

    @Column(name = "y")
    private int y;
}
