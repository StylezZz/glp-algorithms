package com.glp.glpDP1.api.dto.request;

import com.glp.glpDP1.domain.Ubicacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloqueoRequest {
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private List<Ubicacion> nodos;
}