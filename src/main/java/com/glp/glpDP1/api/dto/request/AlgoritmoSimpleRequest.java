package com.glp.glpDP1.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO simplificado para solicitar la ejecución del algoritmo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgoritmoSimpleRequest {
    /**
     * Momento actual para la planificación
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime momentoActual;

    /**
     * Escenario de simulación
     */
    private String escenario;

    /**
     * Parámetros para algoritmo genético
     */
    private Integer tamañoPoblacion;
    private Integer numGeneraciones;
    private Double tasaMutacion;
    private Double tasaCruce;
    private Integer elitismo;
}