package com.glp.glpDP1.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para solicitudes del algoritmo ACO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACORequest {

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
     * Número de hormigas en la colonia
     */
    private Integer numHormigas;

    /**
     * Número máximo de iteraciones
     */
    private Integer numIteraciones;

    /**
     * Importancia de las feromonas
     */
    private Double alfa;

    /**
     * Importancia de la heurística
     */
    private Double beta;

    /**
     * Tasa de evaporación de feromonas
     */
    private Double rho;

    /**
     * Parámetro de exploración vs explotación (0-1)
     */
    private Double q0;
}