package com.glp.glpDP1.api.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PedidoResponse {
    private String id;
    private String idCliente;
    private double cantidadGLP;
    private LocalDateTime horaRecepcion;
    private LocalDateTime horaEntregaProgramada;
    private LocalDateTime horaEntregaReal;
    private boolean entregado;
    private String camionAsignado;
}
