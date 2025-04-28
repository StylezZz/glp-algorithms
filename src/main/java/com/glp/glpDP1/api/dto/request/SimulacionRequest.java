package com.glp.glpDP1.api.dto.request;

import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulacionRequest {
    private EscenarioSimulacion escenario;
    private LocalDateTime fechaInicio;
    private Map<String, Object> parametros = new HashMap<>();
}