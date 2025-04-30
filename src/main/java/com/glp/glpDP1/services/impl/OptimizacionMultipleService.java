// Crear un nuevo servicio: OptimizacionMultipleService.java

package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.algorithm.AlgoritmoGenetico;
import com.glp.glpDP1.algorithm.AlgoritmoPSO;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
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
     * Ejecuta el algoritmo genético múltiples veces y devuelve la mejor solución
     */
    public ResultadoOptimizacion ejecutarMultipleGenetico(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            int numEjecuciones) {

        return ejecutarMultiple("GENETICO", camiones, pedidos, mapa, momento, escenario, numEjecuciones);
    }

    /**
     * Ejecuta el algoritmo PSO múltiples veces y devuelve la mejor solución
     */
    public ResultadoOptimizacion ejecutarMultiplePSO(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            int numEjecuciones) {

        return ejecutarMultiple("PSO", camiones, pedidos, mapa, momento, escenario, numEjecuciones);
    }

    /**
     * Método genérico para ejecutar cualquier algoritmo múltiples veces
     */
    private ResultadoOptimizacion ejecutarMultiple(
            String tipoAlgoritmo,
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            int numEjecuciones) {

        // Crear pool de hilos para ejecuciones paralelas
        ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(numEjecuciones, Runtime.getRuntime().availableProcessors())
        );

        // Lista de tareas
        List<Callable<ResultadoOptimizacion>> tareas = new ArrayList<>();

        // Crear tareas para cada ejecución
        for (int i = 0; i < numEjecuciones; i++) {
            final int ejecucion = i;

            Callable<ResultadoOptimizacion> tarea = () -> {
                long tiempoInicio = System.currentTimeMillis();
                List<Ruta> rutas;
                double fitness;

                if ("GENETICO".equals(tipoAlgoritmo)) {
                    AlgoritmoGenetico algoritmo = new AlgoritmoGenetico();
                    rutas = algoritmo.optimizarRutas(camiones, pedidos, mapa, momento);
                    fitness = algoritmo.getMejorFitness();
                } else {
                    AlgoritmoPSO algoritmo = new AlgoritmoPSO();
                    rutas = algoritmo.optimizarRutas(camiones, pedidos, mapa, momento);
                    fitness = algoritmo.getMejorFitness();
                }

                long tiempoFin = System.currentTimeMillis();
                long duracion = tiempoFin - tiempoInicio;

                int pedidosAsignados = rutas.stream()
                        .mapToInt(r -> r.getPedidosAsignados().size())
                        .sum();

                return new ResultadoOptimizacion(ejecucion, rutas, fitness, pedidosAsignados, duracion);
            };

            tareas.add(tarea);
        }

        try {
            // Ejecutar todas las tareas y obtener resultados
            List<Future<ResultadoOptimizacion>> resultados = executorService.invokeAll(tareas);

            // Encontrar la mejor solución
            ResultadoOptimizacion mejorResultado = null;

            for (Future<ResultadoOptimizacion> futuro : resultados) {
                ResultadoOptimizacion resultado = futuro.get();

                if (mejorResultado == null ||
                        resultado.pedidosAsignados > mejorResultado.pedidosAsignados ||
                        (resultado.pedidosAsignados == mejorResultado.pedidosAsignados &&
                                resultado.fitness < mejorResultado.fitness)) {
                    mejorResultado = resultado;
                }
            }

            // Resumen de todas las ejecuciones
            System.out.println("===== Resumen de ejecuciones múltiples =====");
            System.out.println("Total ejecuciones: " + numEjecuciones);
            System.out.println("Mejor ejecución: " + mejorResultado.ejecucion);
            System.out.println("Mejor fitness: " + mejorResultado.fitness);
            System.out.println("Pedidos asignados: " + mejorResultado.pedidosAsignados);

            return mejorResultado;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            executorService.shutdown();
        }
    }

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
