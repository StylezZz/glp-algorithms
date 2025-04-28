package com.glp.glpDP1.api.dto.response;

import com.glp.glpDP1.domain.Ruta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgoritmoResultResponse {
    private String id;
    private List<Ruta> rutas;
    private double fitness; // Valor de aptitud (menor es mejor)
    private double distanciaTotal; // Km totales recorridos
    private double consumoCombustible; // Galones totales consumidos
    private int pedidosEntregados;
    private int pedidosTotales;
    private Map<String, Object> metricas; // Métricas adicionales
    private String tipoAlgoritmo; // "GENETICO" o "PSO"
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Duration tiempoEjecucion;
}