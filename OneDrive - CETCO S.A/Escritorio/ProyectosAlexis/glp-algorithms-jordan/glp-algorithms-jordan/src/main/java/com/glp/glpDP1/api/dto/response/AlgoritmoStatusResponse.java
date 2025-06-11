package com.glp.glpDP1.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgoritmoStatusResponse {
    private String id;
    private EstadoAlgoritmo estado;
    private double progreso; // De 0 a 100
    private LocalDateTime horaInicio;
    private LocalDateTime horaUltimaActualizacion;
    private Double mejorFitness; // Null si aún no hay resultados

    public enum EstadoAlgoritmo {
        PENDIENTE,
        EN_EJECUCION,
        COMPLETADO,
        FALLIDO
    }
}