// Crear un nuevo servicio: OptimizacionMultipleService.java

package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.algorithm.AlgoritmoGenetico;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;

@Service
public class OptimizacionMultipleService {

    /**
     * Clase para almacenar resultados de optimización
     */
    public static class ResultadoOptimizacion {
        public final int ejecucion;
        public final List<Ruta> rutas;
        public final double fitness;
        public final int pedidosAsignados;
        public final long tiempoEjecucionMs;

        public ResultadoOptimizacion(int ejecucion, List<Ruta> rutas, double fitness,
                                     int pedidosAsignados, long tiempoEjecucionMs) {
            this.ejecucion = ejecucion;
            this.rutas = rutas;
            this.fitness = fitness;
            this.pedidosAsignados = pedidosAsignados;
            this.tiempoEjecucionMs = tiempoEjecucionMs;
        }
    }
}
