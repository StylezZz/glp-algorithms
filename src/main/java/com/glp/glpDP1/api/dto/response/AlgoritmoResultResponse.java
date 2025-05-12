package com.glp.glpDP1.api.dto.response;

import com.glp.glpDP1.domain.Ruta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
    private List<Ruta> rutasTrasvase = new ArrayList<>();

    public AlgoritmoResultResponse(String id, List<Ruta> rutas, double fitness, double distanciaTotal,
            double consumoCombustible, int pedidosEntregados, int pedidosTotales,
            Map<String, Object> metricas, String tipoAlgoritmo, LocalDateTime horaInicio,
            LocalDateTime horaFin, Duration tiempoEjecucion) {
        this.id = id;
        this.rutas = rutas;
        this.fitness = fitness;
        this.distanciaTotal = distanciaTotal;
        this.consumoCombustible = consumoCombustible;
        this.pedidosEntregados = pedidosEntregados;
        this.pedidosTotales = pedidosTotales;
        this.metricas = metricas;
        this.tipoAlgoritmo = tipoAlgoritmo;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.tiempoEjecucion = tiempoEjecucion;
        this.rutasTrasvase = new ArrayList<>();
    }

    public void agregarRutaTrasvase(Ruta ruta) {
        if (this.rutasTrasvase == null) {
            this.rutasTrasvase = new ArrayList<>();
        }
        this.rutasTrasvase.add(ruta);
    }
}