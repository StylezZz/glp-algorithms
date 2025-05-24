package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.algorithm.AlgoritmoGenetico;
import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para ejecutar la simulación semanal
 */
@Service
@Slf4j
public class SimulacionSemanalService {

    private final AveriaService averiaService;
    private final MonitoreoService monitoreoService;

    @Autowired
    private DataRepositoryImpl dataRepository;

    public SimulacionSemanalService(AveriaService averiaService, MonitoreoService monitoreoService) {
        this.averiaService = averiaService;
        this.monitoreoService = monitoreoService;
    }

    /**
     * Ejecuta la simulación semanal (7 días)
     * 
     * @param camionesIniciales Lista de camiones disponibles al inicio
     * @param pedidosTotales    Lista de todos los pedidos para la semana
     * @param mapa              Mapa de la ciudad
     * @param fechaInicio       Fecha de inicio de la simulación
     * @return Resultados de la simulación
     */
    public Map<String, Object> ejecutarSimulacionSemanal(
            List<Camion> camionesIniciales,
            List<Pedido> pedidosTotales,
            Mapa mapa,
            LocalDateTime fechaInicio) {

        // Medir tiempo de inicio de la simulación completa
        long tiempoInicioMs = System.currentTimeMillis();

        log.info("Iniciando simulación semanal desde {}",
                fechaInicio.format(DateTimeFormatter.ISO_DATE_TIME));

        // Validar camiones
        if (camionesIniciales == null || camionesIniciales.isEmpty()) {
            throw new IllegalArgumentException("No hay camiones disponibles para la simulación");
        }

        // Validar pedidos
        if (pedidosTotales == null || pedidosTotales.isEmpty()) {
            throw new IllegalArgumentException("No hay pedidos para simular");
        }

        // Clonar camiones para no modificar los originales
        List<Camion> camiones = clonarCamiones(camionesIniciales);

        // Ordenar pedidos por fecha de recepción
        List<Pedido> pedidosOrdenados = pedidosTotales.stream()
                .sorted(Comparator.comparing(Pedido::getHoraRecepcion))
                .collect(Collectors.toList());

        // Variables para estadísticas
        Map<String, Object> estadisticas = new HashMap<>();
        int pedidosAsignados = 0;
        int pedidosEntregados = 0;
        int pedidosRetrasados = 0;
        double distanciaTotal = 0;
        double consumoCombustible = 0;
        int averiasOcurridas = 0;

        // Variables para métricas de fitness
        double fitnessSemanalAcumulado = 0.0;
        double fitnessSemanalMejor = Double.MAX_VALUE;
        double fitnessSemanalPeor = 0.0;
        List<Double> fitnessValoresDiarios = new ArrayList<>();

        // Variables para tiempos de ejecución por día
        Map<Integer, Long> tiempoEjecucionPorDia = new HashMap<>();
        long tiempoTotalAlgoritmosMs = 0;

        // Resultados por día
        Map<Integer, Map<String, Object>> resultadosPorDia = new HashMap<>();

        // Simulación día a día
        LocalDateTime fechaActual = fechaInicio;
        LocalDateTime fechaFin = fechaInicio.plusDays(7);

        int diaActual = 1;
        while (fechaActual.isBefore(fechaFin)) {
            log.info("==================================================");
            log.info("SIMULANDO DÍA {}: {}", diaActual, fechaActual.toLocalDate());

            // Medir tiempo de inicio del algoritmo diario
            long tiempoInicioDiaMs = System.currentTimeMillis();

            // Actualizar estado de camiones (mantenimiento, averías, etc.)
            actualizarEstadoCamiones(camiones, fechaActual);
            imprimirEstadoFlota(camiones);

            // Filtrar pedidos para el día actual
            List<Pedido> pedidosDia = filtrarPedidosDia(pedidosOrdenados, fechaActual);

            if (!pedidosDia.isEmpty()) {
                log.info("Pedidos para hoy: {}", pedidosDia.size());

                // Ejecutar el algoritmo genético para el día
                Map<String, Object> resultadoDia = ejecutarAlgoritmoDia(
                        camiones, pedidosDia, mapa, fechaActual);

                // Actualizar estadísticas
                pedidosAsignados += (int) resultadoDia.getOrDefault("pedidosAsignados", 0);
                pedidosEntregados += (int) resultadoDia.getOrDefault("pedidosEntregados", 0);
                pedidosRetrasados += (int) resultadoDia.getOrDefault("pedidosRetrasados", 0);
                distanciaTotal += (double) resultadoDia.getOrDefault("distanciaTotal", 0.0);
                consumoCombustible += (double) resultadoDia.getOrDefault("consumoCombustible", 0.0);
                averiasOcurridas += (int) resultadoDia.getOrDefault("averiasOcurridas", 0);

                // Actualizar métricas de fitness
                if (resultadoDia.containsKey("fitness")) {
                    double fitnessDia = (double) resultadoDia.get("fitness");

                    // Actualizar fitness acumulado
                    fitnessSemanalAcumulado += fitnessDia;

                    // Actualizar mejor fitness (menor valor es mejor)
                    if (fitnessDia < fitnessSemanalMejor) {
                        fitnessSemanalMejor = fitnessDia;
                    }

                    // Actualizar peor fitness
                    if (fitnessDia > fitnessSemanalPeor) {
                        fitnessSemanalPeor = fitnessDia;
                    }

                    // Agregar a la lista de valores diarios
                    fitnessValoresDiarios.add(fitnessDia);
                }

                // Medir tiempo de fin del algoritmo diario
                long tiempoFinDiaMs = System.currentTimeMillis();
                long duracionDiaMs = tiempoFinDiaMs - tiempoInicioDiaMs;

                // Guardar tiempo de ejecución del día
                tiempoEjecucionPorDia.put(diaActual, duracionDiaMs);
                tiempoTotalAlgoritmosMs += duracionDiaMs;

                // Añadir tiempo de ejecución al resultado del día
                resultadoDia.put("tiempoEjecucionMs", duracionDiaMs);

                // Guardar resultados del día
                resultadosPorDia.put(diaActual, resultadoDia);

                log.info(
                        "Día {}: {} pedidos asignados, {} entregados, {} retrasados, {} averías, fitness: {}, tiempo: {} ms",
                        diaActual,
                        resultadoDia.getOrDefault("pedidosAsignados", 0),
                        resultadoDia.getOrDefault("pedidosEntregados", 0),
                        resultadoDia.getOrDefault("pedidosRetrasados", 0),
                        resultadoDia.getOrDefault("averiasOcurridas", 0),
                        resultadoDia.containsKey("fitness") ? resultadoDia.get("fitness") : "N/A",
                        duracionDiaMs);
            } else {
                log.info("No hay pedidos para el día de hoy");
                // Guardar tiempo de ejecución vacío
                tiempoEjecucionPorDia.put(diaActual, 0L);
            }

            // Avanzar al siguiente día
            fechaActual = fechaActual.plusDays(1);
            diaActual++;
        }

        // Calcular fitness promedio semanal
        double fitnessSemanalPromedio = fitnessValoresDiarios.isEmpty() ? 0.0
                : fitnessValoresDiarios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calcular fitness ponderado (fitness/pedido entregado)
        double fitnessPorPedido = pedidosEntregados > 0 ? fitnessSemanalAcumulado / pedidosEntregados : 0.0;

        // Medir tiempo de fin de la simulación completa
        long tiempoFinMs = System.currentTimeMillis();
        long tiempoTotalSimulacionMs = tiempoFinMs - tiempoInicioMs;

        // Calcular tiempos y porcentajes
        Map<String, Object> metricas_tiempo = new HashMap<>();
        metricas_tiempo.put("tiempoTotalSimulacionMs", tiempoTotalSimulacionMs);
        metricas_tiempo.put("tiempoTotalAlgoritmosMs", tiempoTotalAlgoritmosMs);
        metricas_tiempo.put("tiempoPromedioEjecucionDiariaMs", tiempoTotalAlgoritmosMs / 7); // Promedio por día
        metricas_tiempo.put("tiempoOverheadMs", tiempoTotalSimulacionMs - tiempoTotalAlgoritmosMs); // Overhead fuera de
                                                                                                    // los algoritmos
        metricas_tiempo.put("tiempoPorDiaMs", tiempoEjecucionPorDia);

        // Registrar máximos y mínimos
        long tiempoMaximoDiaMs = tiempoEjecucionPorDia.values().stream().mapToLong(Long::longValue).max().orElse(0);
        long tiempoMinimoDiaMs = tiempoEjecucionPorDia.values().stream()
                .filter(tiempo -> tiempo > 0) // Excluir días sin pedidos
                .mapToLong(Long::longValue).min().orElse(0);
        metricas_tiempo.put("diaMasRapidoMs", tiempoMinimoDiaMs);
        metricas_tiempo.put("diaMasLentoMs", tiempoMaximoDiaMs);

        // Día con mayor carga de trabajo
        Optional<Map.Entry<Integer, Long>> diaMaximo = tiempoEjecucionPorDia.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        if (diaMaximo.isPresent()) {
            metricas_tiempo.put("diaMayorCarga", diaMaximo.get().getKey());
            metricas_tiempo.put("tiempoDiaMayorCargaMs", diaMaximo.get().getValue());
        }

        // Compilar estadísticas finales
        estadisticas.put("pedidosTotales", pedidosTotales.size());
        estadisticas.put("pedidosAsignados", pedidosAsignados);
        estadisticas.put("pedidosEntregados", pedidosEntregados);
        estadisticas.put("pedidosRetrasados", pedidosRetrasados);
        estadisticas.put("porcentajeEntrega", (double) pedidosEntregados / pedidosTotales.size() * 100);
        estadisticas.put("distanciaTotal", distanciaTotal);
        estadisticas.put("consumoCombustible", consumoCombustible);
        estadisticas.put("averiasOcurridas", averiasOcurridas);

        // Agregar métricas de fitness
        Map<String, Object> metricasFitness = new HashMap<>();
        metricasFitness.put("fitnessSemanalAcumulado", fitnessSemanalAcumulado);
        metricasFitness.put("fitnessSemanalPromedio", fitnessSemanalPromedio);
        metricasFitness.put("fitnessSemanalMejor", fitnessSemanalMejor);
        metricasFitness.put("fitnessSemanalPeor", fitnessSemanalPeor);
        metricasFitness.put("fitnessPorPedido", fitnessPorPedido);
        metricasFitness.put("fitnessValoresDiarios", fitnessValoresDiarios);

        estadisticas.put("fitness", metricasFitness);
        estadisticas.put("resultadosPorDia", resultadosPorDia);
        estadisticas.put("tiempoEjecucion", metricas_tiempo);

        // Agregar indicador general de calidad de la solución semanal
        // Combinación de porcentaje de entrega y fitness
        double porcentajeEntrega = (double) pedidosEntregados / pedidosTotales.size() * 100;
        double calidadSolucion = pedidosTotales.isEmpty() ? 0.0
                : (porcentajeEntrega * 0.7) - (fitnessSemanalPromedio / pedidosTotales.size() * 0.3);

        estadisticas.put("calidadSolucionSemanal", calidadSolucion);

        // Calcular eficiencia en tiempo-costo-demanda
        double eficienciaTiempo = tiempoTotalAlgoritmosMs > 0
                ? (double) pedidosEntregados / tiempoTotalAlgoritmosMs * 1000
                : 0; // Pedidos entregados por segundo
        estadisticas.put("eficienciaTiempo", eficienciaTiempo);

        log.info("======== FIN DE SIMULACIÓN SEMANAL ========");
        log.info("Total pedidos: {}, Asignados: {}, Entregados: {}, Retrasados: {}",
                pedidosTotales.size(), pedidosAsignados, pedidosEntregados, pedidosRetrasados);
        log.info("Distancia total: {:.2f} km, Consumo: {:.2f} gal, Averías: {}",
                distanciaTotal, consumoCombustible, averiasOcurridas);
        log.info("Fitness semanal - Acumulado: {:.2f}, Promedio: {:.2f}, Mejor: {:.2f}, Peor: {:.2f}",
                fitnessSemanalAcumulado, fitnessSemanalPromedio, fitnessSemanalMejor, fitnessSemanalPeor);
        log.info("Tiempo total de simulación: {} ms, Tiempo algoritmos: {} ms",
                tiempoTotalSimulacionMs, tiempoTotalAlgoritmosMs);
        log.info("Calidad general de la solución semanal: {:.2f}", calidadSolucion);

        return estadisticas;
    }

    /**
     * Imprime el estado actual de la flota de camiones
     */
    private void imprimirEstadoFlota(List<Camion> camiones) {
        Map<EstadoCamion, List<String>> estadoCamiones = new HashMap<>();

        for (Camion camion : camiones) {
            estadoCamiones.computeIfAbsent(camion.getEstado(), k -> new ArrayList<>())
                    .add(camion.getCodigo());
        }

        for (Map.Entry<EstadoCamion, List<String>> entry : estadoCamiones.entrySet()) {
            log.info("Camiones {}: {} ({} unidades)",
                    entry.getKey(),
                    String.join(", ", entry.getValue()),
                    entry.getValue().size());
        }

        int disponibles = (int) camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE)
                .count();

        log.info("Estado de flota: {}/{} disponibles", disponibles, camiones.size());
    }

    /**
     * Ejecuta el algoritmo para un día específico
     */
    private Map<String, Object> ejecutarAlgoritmoDia(
            List<Camion> camiones,
            List<Pedido> pedidosDia,
            Mapa mapa,
            LocalDateTime fechaDia) {

        Map<String, Object> resultado = new HashMap<>();

        // Filtrar camiones disponibles
        List<Camion> camionesDisponibles = camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE)
                .collect(Collectors.toList());

        log.info("Camiones disponibles: {}/{}", camionesDisponibles.size(), camiones.size());

        // Mostrar detalle de camiones en mantenimiento
        List<String> camionesEnMantenimiento = camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.EN_MANTENIMIENTO)
                .map(Camion::getCodigo)
                .collect(Collectors.toList());

        if (!camionesEnMantenimiento.isEmpty()) {
            log.info("Camiones en mantenimiento: {}", String.join(", ", camionesEnMantenimiento));
        }

        // Mostrar camiones con averías programadas
        if (averiaService != null) {
            List<String> camionesConAverias = new ArrayList<>();

            // Para cada camión disponible, verificar si tiene avería programada hoy
            for (Camion camion : camionesDisponibles) {
                // Comprobar los tres turnos
                for (Turno turno : Turno.values()) {
                    LocalDateTime momentoTurno = fechaDia.withHour(turno.getHoraInicio() + 1);
                    if (averiaService.tieneProgramadaAveria(camion.getCodigo(), momentoTurno)) {
                        TipoIncidente tipo = averiaService.obtenerIncidenteProgramado(camion.getCodigo(), turno);
                        camionesConAverias.add(camion.getCodigo() + " (" + turno + ", " + tipo + ")");
                    }
                }
            }

            if (!camionesConAverias.isEmpty()) {
                log.info("Camiones con averías programadas hoy: {}", String.join(", ", camionesConAverias));
            }
        }

        // Configurar algoritmo genético
        AlgoritmoGenetico algoritmo = new AlgoritmoGenetico(200, 100, 0.1, 0.85, 15);
        algoritmo.setMonitoreoService(monitoreoService);

        // Ejecutar optimización
        List<Ruta> rutas = algoritmo.optimizarRutas(camionesDisponibles, pedidosDia, mapa, fechaDia);
        log.info("Rutas generadas: {}", rutas.size());

        // Verificar asignación de pedidos
        int totalPedidosAsignados = 0;
        for (Ruta ruta : rutas) {
            totalPedidosAsignados += ruta.getPedidosAsignados().size();
        }
        log.info("Pedidos asignados: {}/{}", totalPedidosAsignados, pedidosDia.size());

        // Simular entregas con posibles averías
        SimuladorEntregas simulador = new SimuladorEntregas(averiaService, dataRepository);
        rutas = simulador.simularEntregas(rutas, fechaDia, mapa, true);

        // Contar averías ocurridas
        int averiasOcurridas = 0;
        for (Ruta ruta : rutas) {
            for (EventoRuta evento : ruta.getEventos()) {
                if (evento.getTipo() == EventoRuta.TipoEvento.AVERIA) {
                    averiasOcurridas++;
                    log.info("Avería ocurrida en la ruta {}: {}", ruta.getCodigoCamion(), evento.getDescripcion());
                }
            }
        }

        // Contar pedidos entregados y retrasados
        int pedidosEntregados = 0;
        int pedidosRetrasados = 0;

        for (Ruta ruta : rutas) {
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (pedido.isEntregado()) {
                    pedidosEntregados++;

                    if (pedido.getHoraEntregaReal().isAfter(pedido.getHoraLimiteEntrega())) {
                        pedidosRetrasados++;
                    }
                }
            }
        }

        // Calcular métricas
        double distanciaTotal = 0;
        double consumoCombustible = 0;

        for (Ruta ruta : rutas) {
            distanciaTotal += ruta.getDistanciaTotal();
            consumoCombustible += ruta.getConsumoCombustible();
        }

        // Actualizar estado de los camiones
        actualizarCamionesDespuesDeRutas(camiones, rutas);

        // Compilar resultados
        resultado.put("rutas", rutas);
        resultado.put("pedidosAsignados", totalPedidosAsignados);
        resultado.put("pedidosEntregados", pedidosEntregados);
        resultado.put("pedidosRetrasados", pedidosRetrasados);
        resultado.put("distanciaTotal", distanciaTotal);
        resultado.put("consumoCombustible", consumoCombustible);
        resultado.put("averiasOcurridas", averiasOcurridas);
        resultado.put("camionesEnMantenimiento", camionesEnMantenimiento);
        resultado.put("fitness", algoritmo.getMejorFitness());

        return resultado;
    }

    /**
     * Actualiza el estado de todos los camiones según fecha
     */
    private void actualizarEstadoCamiones(List<Camion> camiones, LocalDateTime fecha) {
        for (Camion camion : camiones) {
            LocalDateTime momentoAntes = fecha.withHour(0).withMinute(0).withSecond(0);

            EstadoCamion estadoAntes = camion.getEstado();
            camion.actualizarEstado(momentoAntes);
            EstadoCamion estadoDespues = camion.getEstado();

            if (estadoAntes != estadoDespues) {
                log.info("Camión {} cambió de estado: {} -> {}",
                        camion.getCodigo(), estadoAntes, estadoDespues);
            }
        }
    }

    /**
     * Actualiza el estado de los camiones después de ejecutar las rutas
     */
    private void actualizarCamionesDespuesDeRutas(List<Camion> camiones, List<Ruta> rutas) {
        // Para cada ruta, verificar si hubo averías
        for (Ruta ruta : rutas) {
            boolean tuvoAveria = false;

            for (EventoRuta evento : ruta.getEventos()) {
                if (evento.getTipo() == EventoRuta.TipoEvento.AVERIA) {
                    tuvoAveria = true;
                    break;
                }
            }

            if (tuvoAveria) {
                // Buscar el camión correspondiente
                for (Camion camion : camiones) {
                    if (camion.getCodigo().equals(ruta.getCodigoCamion())) {
                        // Actualizar estado según la avería
                        camion.actualizarEstado(LocalDateTime.now());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Filtra los pedidos para un día específico
     */
    private List<Pedido> filtrarPedidosDia(List<Pedido> pedidos, LocalDateTime fecha) {
        return pedidos.stream()
                .filter(p -> mismodia(p.getHoraRecepcion(), fecha))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si dos fechas corresponden al mismo día
     */
    private boolean mismodia(LocalDateTime fecha1, LocalDateTime fecha2) {
        return fecha1.toLocalDate().isEqual(fecha2.toLocalDate());
    }

    /**
     * Clona la lista de camiones para no modificar los originales
     */
    private List<Camion> clonarCamiones(List<Camion> camiones) {
        // En una implementación real, crearíamos copias completas de los camiones
        // Para esta demo, usamos los mismos objetos
        return new ArrayList<>(camiones);
    }
}