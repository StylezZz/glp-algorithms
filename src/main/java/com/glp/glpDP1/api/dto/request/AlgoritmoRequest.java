package com.glp.glpDP1.api.dto.request;

import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgoritmoRequest {
    private List<Camion> camiones;
    private List<Pedido> pedidos;
    private Mapa mapa;
    private LocalDateTime momentoActual;
    private EscenarioSimulacion escenario;

    // Parámetros para algoritmo genético
    private Integer tamañoPoblacion;
    private Integer numGeneraciones;
    private Double tasaMutacion;
    private Double tasaCruce;
    private Integer elitismo;

}