package com.glp.glpDP1.api.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private String tipoSimulador; // "diario" o "semanal"
    
   
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate fecha; // ahora es LocalDate
    
    public String getTipoSimulador() {
        return tipoSimulador;
    }
    public void setTipoSimulador(String tipoSimulador) {
        this.tipoSimulador = tipoSimulador;
    }

    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
    
}