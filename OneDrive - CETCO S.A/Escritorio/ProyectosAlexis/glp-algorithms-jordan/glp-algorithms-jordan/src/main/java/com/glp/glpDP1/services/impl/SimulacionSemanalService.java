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
 * Servicio mejorado para ejecutar la simulación semanal con datos estructurados para visualización
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
     * Ejecuta la simulación semanal (7 días) con datos estructurados para visualización
     *
     * @param camionesIniciales Lista de camiones disponibles al inicio
     * @param pedidosTotales    Lista de todos los pedidos para la semana
     * @param mapa              Mapa de la ciudad
     * @param fechaInicio       Fecha de inicio de la simulación
     * @return Resultados de la simulación estructurados para el frontend
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

        // Validar entrada
        validarParametrosEntrada(camionesIniciales, pedidosTotales, mapa);

        // Clonar camiones para no modificar los originales
        List<Camion> camiones = clonarCamiones(camionesIniciales);

        // Ordenar pedidos por fecha de recepción
        List<Pedido> pedidosOrdenados = pedidosTotales.stream()
                .sorted(Comparator.comparing(Pedido::getHoraRecepcion))
                .collect(Collectors.toList());

        // Inicializar contenedores de resultados
        ResultadosSimulacion resultados = inicializarResultados(fechaInicio);

        // Simulación día a día
        LocalDateTime fechaActual = fechaInicio;
        LocalDateTime fechaFin = fechaInicio.plusDays(7);

        int diaActual = 1;
        while (fechaActual.isBefore(fechaFin)) {
            log.info("==================================================");
            log.info("SIMULANDO DÍA {}: {}", diaActual, fechaActual.toLocalDate());

            // Procesar día
            ResultadoDia resultadoDia = procesarDia(
                    diaActual, fechaActual, camiones, pedidosOrdenados, mapa, resultados);

            // Almacenar resultados del día
            resultados.resultadosPorDia.put(diaActual, resultadoDia);

            // Actualizar estadísticas acumuladas
            actualizarEstadisticasAcumuladas(resultados, resultadoDia);

            // Avanzar al siguiente día
            fechaActual = fechaActual.plusDays(1);
            diaActual++;
        }

        // Calcular métricas finales
        calcularMetricasFinales(resultados, tiempoInicioMs);

        // Estructurar respuesta para el frontend
        return estructurarRespuestaCompleta(resultados, mapa, camionesIniciales);
    }

    private void validarParametrosEntrada(List<Camion> camiones, List<Pedido> pedidos, Mapa mapa) {
        if (camiones == null || camiones.isEmpty()) {
            throw new IllegalArgumentException("No hay camiones disponibles para la simulación");
        }
        if (pedidos == null || pedidos.isEmpty()) {
            throw new IllegalArgumentException("No hay pedidos para simular");
        }
        if (mapa == null) {
            throw new IllegalArgumentException("No hay mapa configurado");
        }
    }

    private ResultadosSimulacion inicializarResultados(LocalDateTime fechaInicio) {
        ResultadosSimulacion resultados = new ResultadosSimulacion();
        resultados.fechaInicio = fechaInicio;
        resultados.fechaFin = fechaInicio.plusDays(7);
        resultados.resultadosPorDia = new HashMap<>();
        resultados.fitnessValoresDiarios = new ArrayList<>();
        resultados.tiempoEjecucionPorDia = new HashMap<>();
        return resultados;
    }

    private ResultadoDia procesarDia(int numeroDia, LocalDateTime fechaDia, List<Camion> camiones,
                                     List<Pedido> pedidosOrdenados, Mapa mapa, ResultadosSimulacion resultados) {

        long tiempoInicioDiaMs = System.currentTimeMillis();

        // Actualizar estado de camiones
        actualizarEstadoCamiones(camiones, fechaDia);
        imprimirEstadoFlota(camiones);

        // Filtrar pedidos para el día actual
        List<Pedido> pedidosDia = filtrarPedidosDia(pedidosOrdenados, fechaDia);

        ResultadoDia resultadoDia = new ResultadoDia();
        resultadoDia.numeroDia = numeroDia;
        resultadoDia.fecha = fechaDia;
        resultadoDia.pedidosDia = pedidosDia.size();

        if (!pedidosDia.isEmpty()) {
            log.info("Pedidos para hoy: {}", pedidosDia.size());

            // Ejecutar algoritmo genético
            Map<String, Object> resultadoAlgoritmo = ejecutarAlgoritmoDia(camiones, pedidosDia, mapa, fechaDia);

            // Estructurar datos del día
            estructurarResultadoDia(resultadoDia, resultadoAlgoritmo);

            // Actualizar estado de camiones después de las rutas
            actualizarCamionesDespuesDeRutas(camiones, resultadoDia.rutas);
        } else {
            log.info("No hay pedidos para el día de hoy");
            resultadoDia.rutas = new ArrayList<>();
        }

        // Calcular tiempo de ejecución
        long tiempoFinDiaMs = System.currentTimeMillis();
        resultadoDia.tiempoEjecucionMs = tiempoFinDiaMs - tiempoInicioDiaMs;
        resultados.tiempoEjecucionPorDia.put(numeroDia, resultadoDia.tiempoEjecucionMs);

        log.info("Día {}: {} pedidos procesados, {} rutas generadas, tiempo: {} ms",
                numeroDia, pedidosDia.size(), resultadoDia.rutas.size(), resultadoDia.tiempoEjecucionMs);

        return resultadoDia;
    }

    private void estructurarResultadoDia(ResultadoDia resultadoDia, Map<String, Object> resultadoAlgoritmo) {
        resultadoDia.rutas = (List<Ruta>) resultadoAlgoritmo.getOrDefault("rutas", new ArrayList<>());
        resultadoDia.pedidosAsignados = (int) resultadoAlgoritmo.getOrDefault("pedidosAsignados", 0);
        resultadoDia.pedidosEntregados = (int) resultadoAlgoritmo.getOrDefault("pedidosEntregados", 0);
        resultadoDia.pedidosRetrasados = (int) resultadoAlgoritmo.getOrDefault("pedidosRetrasados", 0);
        resultadoDia.distanciaTotal = (double) resultadoAlgoritmo.getOrDefault("distanciaTotal", 0.0);
        resultadoDia.consumoCombustible = (double) resultadoAlgoritmo.getOrDefault("consumoCombustible", 0.0);
        resultadoDia.averiasOcurridas = (int) resultadoAlgoritmo.getOrDefault("averiasOcurridas", 0);
        resultadoDia.fitness = (double) resultadoAlgoritmo.getOrDefault("fitness", 0.0);
        resultadoDia.camionesEnMantenimiento = (List<String>) resultadoAlgoritmo.getOrDefault("camionesEnMantenimiento", new ArrayList<>());

        // Calcular camiones con averías programadas
        resultadoDia.camionesConAverias = calcularCamionesConAverias(resultadoDia.fecha);
    }

    private List<String> calcularCamionesConAverias(LocalDateTime fecha) {
        List<String> camionesConAverias = new ArrayList<>();

        if (averiaService != null && dataRepository != null) {
            List<Camion> camiones = dataRepository.obtenerCamiones();

            for (Camion camion : camiones) {
                for (Turno turno : Turno.values()) {
                    LocalDateTime momentoTurno = fecha.withHour(turno.getHoraInicio() + 1);
                    if (averiaService.tieneProgramadaAveria(camion.getCodigo(), momentoTurno)) {
                        TipoIncidente tipo = averiaService.obtenerIncidenteProgramado(camion.getCodigo(), turno);
                        camionesConAverias.add(camion.getCodigo() + " (" + turno + ", " + tipo + ")");
                    }
                }
            }
        }

        return camionesConAverias;
    }

    private void actualizarEstadisticasAcumuladas(ResultadosSimulacion resultados, ResultadoDia resultadoDia) {
        resultados.pedidosAsignados += resultadoDia.pedidosAsignados;
        resultados.pedidosEntregados += resultadoDia.pedidosEntregados;
        resultados.pedidosRetrasados += resultadoDia.pedidosRetrasados;
        resultados.distanciaTotal += resultadoDia.distanciaTotal;
        resultados.consumoCombustible += resultadoDia.consumoCombustible;
        resultados.averiasOcurridas += resultadoDia.averiasOcurridas;

        // Métricas de fitness
        if (resultadoDia.fitness > 0) {
            resultados.fitnessValoresDiarios.add(resultadoDia.fitness);
            resultados.fitnessSemanalAcumulado += resultadoDia.fitness;

            if (resultadoDia.fitness < resultados.fitnessSemanalMejor) {
                resultados.fitnessSemanalMejor = resultadoDia.fitness;
            }
            if (resultadoDia.fitness > resultados.fitnessSemanalPeor) {
                resultados.fitnessSemanalPeor = resultadoDia.fitness;
            }
        }
    }

    private void calcularMetricasFinales(ResultadosSimulacion resultados, long tiempoInicioMs) {
        long tiempoTotalMs = System.currentTimeMillis() - tiempoInicioMs;
        long tiempoAlgoritmosMs = resultados.tiempoEjecucionPorDia.values().stream().mapToLong(Long::longValue).sum();

        resultados.tiempoTotalSimulacionMs = tiempoTotalMs;
        resultados.tiempoTotalAlgoritmosMs = tiempoAlgoritmosMs;

        // Calcular fitness promedio
        if (!resultados.fitnessValoresDiarios.isEmpty()) {
            resultados.fitnessSemanalPromedio = resultados.fitnessValoresDiarios.stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        // Calcular otras métricas
        int pedidosTotales = resultados.resultadosPorDia.values().stream()
                .mapToInt(dia -> dia.pedidosDia).sum();

        resultados.pedidosTotales = pedidosTotales;
        resultados.porcentajeEntrega = pedidosTotales > 0 ?
                (double) resultados.pedidosEntregados / pedidosTotales * 100 : 0.0;

        resultados.eficienciaTiempo = tiempoAlgoritmosMs > 0 ?
                (double) resultados.pedidosEntregados / tiempoAlgoritmosMs * 1000 : 0.0;

        resultados.calidadSolucionSemanal = calcularCalidadSolucion(resultados);

        log.info("======== FIN DE SIMULACIÓN SEMANAL ========");
        log.info("Métricas finales calculadas - Fitness promedio: {:.2f}, Calidad: {:.2f}",
                resultados.fitnessSemanalPromedio, resultados.calidadSolucionSemanal);
    }

    private double calcularCalidadSolucion(ResultadosSimulacion resultados) {
        double porcentajeEntrega = resultados.porcentajeEntrega;
        double fitnessPorPedido = resultados.pedidosEntregados > 0 ?
                resultados.fitnessSemanalAcumulado / resultados.pedidosEntregados : 0.0;

        return (porcentajeEntrega * 0.7) - (fitnessPorPedido / 1000 * 0.3);
    }

    private Map<String, Object> estructurarRespuestaCompleta(ResultadosSimulacion resultados, Mapa mapa, List<Camion> camionesIniciales) {
        Map<String, Object> respuesta = new HashMap<>();

        // Información básica
        respuesta.put("fechaInicio", resultados.fechaInicio);
        respuesta.put("fechaFin", resultados.fechaFin);
        respuesta.put("duracionSimulacion", "7 días");

        // Estadísticas generales
        respuesta.put("pedidosTotales", resultados.pedidosTotales);
        respuesta.put("pedidosAsignados", resultados.pedidosAsignados);
        respuesta.put("pedidosEntregados", resultados.pedidosEntregados);
        respuesta.put("pedidosRetrasados", resultados.pedidosRetrasados);
        respuesta.put("porcentajeEntrega", resultados.porcentajeEntrega);
        respuesta.put("distanciaTotal", resultados.distanciaTotal);
        respuesta.put("consumoCombustible", resultados.consumoCombustible);
        respuesta.put("averiasOcurridas", resultados.averiasOcurridas);
        respuesta.put("calidadSolucionSemanal", resultados.calidadSolucionSemanal);

        // Métricas de fitness
        Map<String, Object> metricasFitness = new HashMap<>();
        metricasFitness.put("fitnessSemanalAcumulado", resultados.fitnessSemanalAcumulado);
        metricasFitness.put("fitnessSemanalPromedio", resultados.fitnessSemanalPromedio);
        metricasFitness.put("fitnessSemanalMejor", resultados.fitnessSemanalMejor);
        metricasFitness.put("fitnessSemanalPeor", resultados.fitnessSemanalPeor);
        metricasFitness.put("fitnessValoresDiarios", resultados.fitnessValoresDiarios);
        respuesta.put("fitness", metricasFitness);

        // Métricas de tiempo
        Map<String, Object> metricas_tiempo = new HashMap<>();
        metricas_tiempo.put("tiempoTotalSimulacionMs", resultados.tiempoTotalSimulacionMs);
        metricas_tiempo.put("tiempoTotalAlgoritmosMs", resultados.tiempoTotalAlgoritmosMs);
        metricas_tiempo.put("tiempoPromedioEjecucionDiariaMs", resultados.tiempoTotalAlgoritmosMs / 7);
        metricas_tiempo.put("tiempoPorDiaMs", resultados.tiempoEjecucionPorDia);
        respuesta.put("tiempoEjecucion", metricas_tiempo);

        // Eficiencia
        respuesta.put("eficienciaTiempo", resultados.eficienciaTiempo);

        // Resultados por día (convertidos a Map para JSON)
        Map<Integer, Map<String, Object>> resultadosPorDiaMap = new HashMap<>();
        for (Map.Entry<Integer, ResultadoDia> entry : resultados.resultadosPorDia.entrySet()) {
            resultadosPorDiaMap.put(entry.getKey(), convertirResultadoDiaAMap(entry.getValue()));
        }
        respuesta.put("resultadosPorDia", resultadosPorDiaMap);

        return respuesta;
    }

    private Map<String, Object> convertirResultadoDiaAMap(ResultadoDia resultadoDia) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("numeroDia", resultadoDia.numeroDia);
        mapa.put("fecha", resultadoDia.fecha);
        mapa.put("pedidosDia", resultadoDia.pedidosDia);
        mapa.put("rutas", resultadoDia.rutas);
        mapa.put("pedidosAsignados", resultadoDia.pedidosAsignados);
        mapa.put("pedidosEntregados", resultadoDia.pedidosEntregados);
        mapa.put("pedidosRetrasados", resultadoDia.pedidosRetrasados);
        mapa.put("distanciaTotal", resultadoDia.distanciaTotal);
        mapa.put("consumoCombustible", resultadoDia.consumoCombustible);
        mapa.put("averiasOcurridas", resultadoDia.averiasOcurridas);
        mapa.put("fitness", resultadoDia.fitness);
        mapa.put("tiempoEjecucionMs", resultadoDia.tiempoEjecucionMs);
        mapa.put("camionesEnMantenimiento", resultadoDia.camionesEnMantenimiento);
        mapa.put("camionesConAverias", resultadoDia.camionesConAverias);
        return mapa;
    }

    // Métodos auxiliares existentes (sin cambios significativos)
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

        // Registrar camiones en mantenimiento
        List<String> camionesEnMantenimiento = camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.EN_MANTENIMIENTO)
                .map(Camion::getCodigo)
                .collect(Collectors.toList());

        if (!camionesEnMantenimiento.isEmpty()) {
            log.info("Camiones en mantenimiento: {}", String.join(", ", camionesEnMantenimiento));
        }

        // Configurar y ejecutar algoritmo genético
        AlgoritmoGenetico algoritmo = new AlgoritmoGenetico(200, 100, 0.1, 0.85, 15);
        algoritmo.setMonitoreoService(monitoreoService);

        List<Ruta> rutas = algoritmo.optimizarRutas(camionesDisponibles, pedidosDia, mapa, fechaDia);
        log.info("Rutas generadas: {}", rutas.size());

        // Simular entregas con posibles averías
        SimuladorEntregas simulador = new SimuladorEntregas(averiaService, dataRepository);
        rutas = simulador.simularEntregas(rutas, fechaDia, mapa, true);

        // Calcular métricas
        calcularMetricasDia(resultado, rutas, pedidosDia, algoritmo.getMejorFitness(), camionesEnMantenimiento);

        return resultado;
    }

    private void calcularMetricasDia(Map<String, Object> resultado, List<Ruta> rutas,
                                     List<Pedido> pedidosDia, double fitness, List<String> camionesEnMantenimiento) {

        int totalPedidosAsignados = rutas.stream().mapToInt(r -> r.getPedidosAsignados().size()).sum();
        int pedidosEntregados = 0;
        int pedidosRetrasados = 0;
        int averiasOcurridas = 0;
        double distanciaTotal = 0;
        double consumoCombustible = 0;

        for (Ruta ruta : rutas) {
            distanciaTotal += ruta.getDistanciaTotal();
            consumoCombustible += ruta.getConsumoCombustible();

            // Contar averías
            averiasOcurridas += (int) ruta.getEventos().stream()
                    .filter(e -> e.getTipo() == EventoRuta.TipoEvento.AVERIA)
                    .count();

            // Contar entregas
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (pedido.isEntregado()) {
                    pedidosEntregados++;
                    if (pedido.getHoraEntregaReal().isAfter(pedido.getHoraLimiteEntrega())) {
                        pedidosRetrasados++;
                    }
                }
            }
        }

        // Llenar resultado
        resultado.put("rutas", rutas);
        resultado.put("pedidosAsignados", totalPedidosAsignados);
        resultado.put("pedidosEntregados", pedidosEntregados);
        resultado.put("pedidosRetrasados", pedidosRetrasados);
        resultado.put("distanciaTotal", distanciaTotal);
        resultado.put("consumoCombustible", consumoCombustible);
        resultado.put("averiasOcurridas", averiasOcurridas);
        resultado.put("fitness", fitness);
        resultado.put("camionesEnMantenimiento", camionesEnMantenimiento);
    }

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

    private void actualizarCamionesDespuesDeRutas(List<Camion> camiones, List<Ruta> rutas) {
        for (Ruta ruta : rutas) {
            boolean tuvoAveria = ruta.getEventos().stream()
                    .anyMatch(e -> e.getTipo() == EventoRuta.TipoEvento.AVERIA);

            if (tuvoAveria) {
                camiones.stream()
                        .filter(c -> c.getCodigo().equals(ruta.getCodigoCamion()))
                        .findFirst()
                        .ifPresent(c -> c.actualizarEstado(LocalDateTime.now()));
            }
        }
    }

    private List<Pedido> filtrarPedidosDia(List<Pedido> pedidos, LocalDateTime fecha) {
        return pedidos.stream()
                .filter(p -> mismodia(p.getHoraRecepcion(), fecha))
                .collect(Collectors.toList());
    }

    private boolean mismodia(LocalDateTime fecha1, LocalDateTime fecha2) {
        return fecha1.toLocalDate().isEqual(fecha2.toLocalDate());
    }

    private List<Camion> clonarCamiones(List<Camion> camiones) {
        return new ArrayList<>(camiones);
    }

    // Clases internas para estructurar los resultados
    private static class ResultadosSimulacion {
        LocalDateTime fechaInicio;
        LocalDateTime fechaFin;
        Map<Integer, ResultadoDia> resultadosPorDia;

        // Estadísticas acumuladas
        int pedidosTotales = 0;
        int pedidosAsignados = 0;
        int pedidosEntregados = 0;
        int pedidosRetrasados = 0;
        double distanciaTotal = 0.0;
        double consumoCombustible = 0.0;
        int averiasOcurridas = 0;
        double porcentajeEntrega = 0.0;

        // Métricas de fitness
        double fitnessSemanalAcumulado = 0.0;
        double fitnessSemanalPromedio = 0.0;
        double fitnessSemanalMejor = Double.MAX_VALUE;
        double fitnessSemanalPeor = 0.0;
        List<Double> fitnessValoresDiarios;

        // Métricas de tiempo
        long tiempoTotalSimulacionMs = 0;
        long tiempoTotalAlgoritmosMs = 0;
        Map<Integer, Long> tiempoEjecucionPorDia;

        // Métricas de eficiencia
        double eficienciaTiempo = 0.0;
        double calidadSolucionSemanal = 0.0;
    }

    private static class ResultadoDia {
        int numeroDia;
        LocalDateTime fecha;
        int pedidosDia;
        List<Ruta> rutas;
        int pedidosAsignados;
        int pedidosEntregados;
        int pedidosRetrasados;
        double distanciaTotal;
        double consumoCombustible;
        int averiasOcurridas;
        double fitness;
        long tiempoEjecucionMs;
        List<String> camionesEnMantenimiento;
        List<String> camionesConAverias;
    }
}