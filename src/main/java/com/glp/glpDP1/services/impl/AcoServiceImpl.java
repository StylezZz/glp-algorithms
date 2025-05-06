package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.algorithm.AlgoritmoACO;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.services.ACOService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementación del servicio para el algoritmo ACO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcoServiceImpl implements ACOService {

    // Almacena las ejecuciones en curso
    private final Map<String, Future<?>> tareas = new ConcurrentHashMap<>();

    // Almacena el estado de cada ejecución
    private final Map<String, Map<String, Object>> estados = new ConcurrentHashMap<>();

    // Almacena los resultados de las ejecuciones completadas
    private final Map<String, Map<String, Object>> resultados = new ConcurrentHashMap<>();

    // Executor para ejecutar los algoritmos de forma asíncrona
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * Ejecuta el algoritmo ACO para optimizar rutas
     *
     * @param camiones       Lista de camiones disponibles
     * @param pedidos        Lista de pedidos pendientes
     * @param mapa           Mapa con información de la ciudad
     * @param momento        Momento actual para planificación
     * @param escenario      Escenario de simulación
     * @param numHormigas    Número de hormigas en la colonia
     * @param numIteraciones Número máximo de iteraciones
     * @param alfa           Importancia de las feromonas
     * @param beta           Importancia de la heurística
     * @param rho            Tasa de evaporación de feromonas
     * @param q0             Parámetro de exploración vs explotación (0-1)
     * @return ID de la ejecución
     */
    @Override
    public String ejecutarACO(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            Integer numHormigas,
            Integer numIteraciones,
            Double alfa,
            Double beta,
            Double rho,
            Double q0) {

        // Generar un ID único para esta ejecución
        String id = UUID.randomUUID().toString();

        // Crear objeto de estado inicial
        Map<String, Object> estado = new HashMap<>();
        estado.put("id", id);
        estado.put("estado", "PENDIENTE");
        estado.put("progreso", 0.0);
        estado.put("horaInicio", LocalDateTime.now());
        estado.put("horaUltimaActualizacion", LocalDateTime.now());
        estado.put("mejorFitness", null);
        estados.put(id, estado);

        // Ejecutar el algoritmo de forma asíncrona
        Future<?> tarea = executor.submit(() -> {
            try {
                estado.put("estado", "EN_EJECUCION");
                LocalDateTime horaInicioEjecucion = LocalDateTime.now();

                // Inicializar el algoritmo ACO con los parámetros proporcionados
                AlgoritmoACO algoritmo;
                // Configurar parámetros si se proporcionaron
                if (numHormigas != null && numIteraciones != null &&
                        alfa != null && beta != null && rho != null && q0 != null) {
                    algoritmo = new AlgoritmoACO(
                            numHormigas,
                            numIteraciones,
                            alfa,
                            beta,
                            rho,
                            q0,
                            0.1 // valor inicial de feromona predeterminado
                    );
                } else {
                    // Usar parámetros por defecto
                    algoritmo = new AlgoritmoACO();
                }
                // Ejecutar optimización
                List<Ruta> rutas = algoritmo.optimizarRutas(
                        camiones,
                        pedidos,
                        mapa,
                        momento
                );
                double fitness = algoritmo.getMejorFitness();

                LocalDateTime horaFinEjecucion = LocalDateTime.now();
                Duration tiempoEjecucion = Duration.between(horaInicioEjecucion, horaFinEjecucion);

                // Calcular métricas adicionales
                double distanciaTotal = calcularDistanciaTotal(rutas);
                double consumoCombustible = calcularConsumoCombustible(rutas);
                int pedidosEntregados = calcularPedidosEntregados(rutas);

                // Crear objeto de resultado
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("id", id);
                resultado.put("rutas", rutas);
                resultado.put("fitness", fitness);
                resultado.put("distanciaTotal", distanciaTotal);
                resultado.put("consumoCombustible", consumoCombustible);
                resultado.put("pedidosEntregados", pedidosEntregados);
                resultado.put("pedidosTotales", pedidos.size());
                resultado.put("tipoAlgoritmo", "ACO");
                resultado.put("horaInicio", horaInicioEjecucion);
                resultado.put("horaFin", horaFinEjecucion);
                resultado.put("tiempoEjecucion", tiempoEjecucion);

                // Métricas adicionales
                Map<String, Object> metricas = new HashMap<>();
                metricas.put("tiempoEjecucionMs", tiempoEjecucion.toMillis());
                metricas.put("escenarioSimulacion", escenario);
                metricas.put("numHormigas", algoritmo.getNumHormigas());
                metricas.put("numIteraciones", algoritmo.getNumIteraciones());
                resultado.put("metricas", metricas);

                // Guardar resultado
                resultados.put(id, resultado);

                // Actualizar estado a COMPLETADO
                estado.put("estado", "COMPLETADO");
                estado.put("progreso", 100);
                estado.put("horaUltimaActualizacion", horaFinEjecucion);
                estado.put("mejorFitness", fitness);

                log.info("Algoritmo ACO completado para ID {}, fitness: {}", id, fitness);

            } catch (Exception e) {
                // Actualizar estado a FALLIDO
                estado.put("estado", "FALLIDO");
                estado.put("horaUltimaActualizacion", LocalDateTime.now());
                log.error("Error en ejecución de algoritmo ACO {}: {}", id, e.getMessage(), e);
            }
        });

        // Almacenar la tarea
        tareas.put(id, tarea);

        return id;
    }

    /**
     * Ejecuta el algoritmo ACO con parámetros optimizados
     *
     * @param camiones  Lista de camiones disponibles
     * @param pedidos   Lista de pedidos pendientes
     * @param mapa      Mapa con información de la ciudad
     * @param momento   Momento actual para planificación
     * @param escenario Escenario de simulación
     * @return ID de la ejecución
     */
    @Override
    public String ejecutarACOOptimizado(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario) {

        // Parámetros optimizados para ACO basados en pruebas previas
        return ejecutarACO(
                camiones,
                pedidos,
                mapa,
                momento,
                escenario,
                40,    // Más hormigas para mejor exploración
                150,   // Más iteraciones para mejor convergencia
                1.0,   // Alfa: importancia de feromonas
                2.5,   // Beta: mayor importancia a la heurística de distancia/consumo
                0.1,   // Rho: tasa de evaporación de feromonas
                0.9    // q0: 90% explotación, 10% exploración
        );
    }

    /**
     * Obtiene el estado de una ejecución
     *
     * @param id ID de la ejecución
     * @return Estado actual
     */
    @Override
    public Map<String, Object> obtenerEstado(String id) {
        Map<String, Object> estado = estados.get(id);
        if (estado == null) {
            throw new NoSuchElementException("No se encontró la ejecución con ID: " + id);
        }
        return new HashMap<>(estado); // Devolver una copia para evitar modificaciones externas
    }

    /**
     * Obtiene los resultados de una ejecución completada
     *
     * @param id ID de la ejecución
     * @return Resultados de la optimización
     */
    @Override
    public Map<String, Object> obtenerResultados(String id) {
        Map<String, Object> resultado = resultados.get(id);
        if (resultado == null) {
            Map<String, Object> estado = estados.get(id);
            if (estado == null) {
                throw new NoSuchElementException("No se encontró la ejecución con ID: " + id);
            }

            String estadoActual = (String) estado.get("estado");
            if ("EN_EJECUCION".equals(estadoActual) || "PENDIENTE".equals(estadoActual)) {
                throw new IllegalStateException("La ejecución aún no ha terminado");
            } else if ("FALLIDO".equals(estadoActual)) {
                throw new IllegalStateException("La ejecución falló y no generó resultados");
            }

            throw new NoSuchElementException("No se encontraron resultados para la ejecución con ID: " + id);
        }
        return new HashMap<>(resultado); // Devolver una copia para evitar modificaciones externas
    }

    /**
     * Cancela una ejecución en curso
     *
     * @param id ID de la ejecución
     * @return true si se canceló, false si no
     */
    @Override
    public boolean cancelarEjecucion(String id) {
        Future<?> tarea = tareas.get(id);
        if (tarea == null) {
            return false;
        }

        boolean cancelled = tarea.cancel(true);
        if (cancelled) {
            Map<String, Object> estado = estados.get(id);
            if (estado != null) {
                estado.put("estado", "FALLIDO");
                estado.put("horaUltimaActualizacion", LocalDateTime.now());
            }
        }
        return cancelled;
    }

    /**
     * Ejecuta múltiples instancias del algoritmo ACO y devuelve la mejor solución
     *
     * @param camiones       Lista de camiones disponibles
     * @param pedidos        Lista de pedidos pendientes
     * @param mapa           Mapa con información de la ciudad
     * @param momento        Momento actual para planificación
     * @param escenario      Escenario de simulación
     * @param numEjecuciones Número de ejecuciones independientes
     * @return Resultado de la mejor ejecución
     */
    @Override
    public Map<String, Object> ejecutarMultipleACO(
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
        List<Callable<Map<String, Object>>> tareas = new ArrayList<>();

        // Crear tareas para cada ejecución
        for (int i = 0; i < numEjecuciones; i++) {
            final int ejecucion = i;

            Callable<Map<String, Object>> tarea = () -> {
                long tiempoInicio = System.currentTimeMillis();

                // Crear una instancia del algoritmo ACO
                AlgoritmoACO algoritmo = new AlgoritmoACO();
                List<Ruta> rutas = algoritmo.optimizarRutas(camiones, pedidos, mapa, momento);
                double fitness = algoritmo.getMejorFitness();

                long tiempoFin = System.currentTimeMillis();
                long duracion = tiempoFin - tiempoInicio;

                int pedidosAsignados = rutas.stream()
                        .mapToInt(r -> r.getPedidosAsignados().size())
                        .sum();

                double distanciaTotal = calcularDistanciaTotal(rutas);
                double consumoCombustible = calcularConsumoCombustible(rutas);

                Map<String, Object> resultado = new HashMap<>();
                resultado.put("ejecucion", ejecucion);
                resultado.put("rutas", rutas);
                resultado.put("fitness", fitness);
                resultado.put("pedidosAsignados", pedidosAsignados);
                resultado.put("distanciaTotal", distanciaTotal);
                resultado.put("consumoCombustible", consumoCombustible);
                resultado.put("tiempoEjecucionMs", duracion);

                return resultado;
            };

            tareas.add(tarea);
        }

        try {
            // Ejecutar todas las tareas y obtener resultados
            List<Future<Map<String, Object>>> resultados = executorService.invokeAll(tareas);

            // Encontrar la mejor solución
            Map<String, Object> mejorResultado = null;

            for (Future<Map<String, Object>> futuro : resultados) {
                Map<String, Object> resultado = futuro.get();

                if (mejorResultado == null ||
                        (int) resultado.get("pedidosAsignados") > (int) mejorResultado.get("pedidosAsignados") ||
                        ((int) resultado.get("pedidosAsignados") == (int) mejorResultado.get("pedidosAsignados") &&
                                (double) resultado.get("fitness") < (double) mejorResultado.get("fitness"))) {
                    mejorResultado = resultado;
                }
            }

            // Añadir información adicional al resultado
            mejorResultado.put("totalEjecuciones", numEjecuciones);
            mejorResultado.put("momentoActual", momento);
            mejorResultado.put("escenario", escenario);
            mejorResultado.put("tipoAlgoritmo", "ACO_MULTIPLE");
            mejorResultado.put("pedidosTotales", pedidos.size());

            return mejorResultado;

        } catch (Exception e) {
            log.error("Error al ejecutar ACO múltiple: {}", e.getMessage(), e);
            throw new RuntimeException("Error en ejecución múltiple de ACO", e);
        } finally {
            executorService.shutdown();
        }
    }

    // Métodos auxiliares para cálculo de métricas

    private double calcularDistanciaTotal(List<Ruta> rutas) {
        return rutas.stream().mapToDouble(Ruta::getDistanciaTotal).sum();
    }

    private double calcularConsumoCombustible(List<Ruta> rutas) {
        return rutas.stream().mapToDouble(Ruta::getConsumoCombustible).sum();
    }

    private int calcularPedidosEntregados(List<Ruta> rutas) {
        return (int) rutas.stream()
                .flatMap(ruta -> ruta.getPedidosAsignados().stream())
                .map(Pedido::getId)
                .distinct()
                .count();
    }
}