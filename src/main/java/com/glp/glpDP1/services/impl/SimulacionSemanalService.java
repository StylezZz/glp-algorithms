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
     * @param camionesIniciales Lista de camiones disponibles al inicio
     * @param pedidosTotales Lista de todos los pedidos para la semana
     * @param mapa Mapa de la ciudad
     * @param fechaInicio Fecha de inicio de la simulación
     * @return Resultados de la simulación
     */
    public Map<String, Object> ejecutarSimulacionSemanal(
            List<Camion> camionesIniciales,
            List<Pedido> pedidosTotales,
            Mapa mapa,
            LocalDateTime fechaInicio) {

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
        
        // Resultados por día
        Map<Integer, Map<String, Object>> resultadosPorDia = new HashMap<>();
        
        // Simulación día a día
        LocalDateTime fechaActual = fechaInicio;
        LocalDateTime fechaFin = fechaInicio.plusDays(7);
        
        while (fechaActual.isBefore(fechaFin)) {
            log.info("==================================================");
            log.info("SIMULANDO DÍA: {}", fechaActual.toLocalDate());
            
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
                
                // Guardar resultados del día
                int diaSimulacion = (int) ChronoUnit.DAYS.between(fechaInicio.toLocalDate(), 
                                                                 fechaActual.toLocalDate()) + 1;
                resultadosPorDia.put(diaSimulacion, resultadoDia);
                
                log.info("Día {}: {} pedidos asignados, {} entregados, {} retrasados, {} averías", 
                    diaSimulacion, 
                    resultadoDia.getOrDefault("pedidosAsignados", 0),
                    resultadoDia.getOrDefault("pedidosEntregados", 0),
                    resultadoDia.getOrDefault("pedidosRetrasados", 0),
                    resultadoDia.getOrDefault("averiasOcurridas", 0));
            } else {
                log.info("No hay pedidos para el día de hoy");
            }
            
            // Avanzar al siguiente día
            fechaActual = fechaActual.plusDays(1);
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
        estadisticas.put("resultadosPorDia", resultadosPorDia);
        
        log.info("======== FIN DE SIMULACIÓN SEMANAL ========");
        log.info("Total pedidos: {}, Asignados: {}, Entregados: {}, Retrasados: {}", 
                pedidosTotales.size(), pedidosAsignados, pedidosEntregados, pedidosRetrasados);
        log.info("Distancia total: {:.2f} km, Consumo: {:.2f} gal, Averías: {}", 
                distanciaTotal, consumoCombustible, averiasOcurridas);
        
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