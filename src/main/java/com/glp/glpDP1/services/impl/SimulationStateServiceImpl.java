package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.api.dto.websocket.EstadoSimulacionResponse;
import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.AlgoritmoService;
import com.glp.glpDP1.services.SimulationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationStateServiceImpl implements SimulationStateService {

    private final DataRepository dataRepository;
    private final AlgoritmoService algoritmoService;
    private final AveriaService averiaService;
    private final MonitoreoService monitoreoService;

    // Estado actual de la simulación
    private volatile LocalDateTime momentoSimulacionActual;
    private volatile boolean simulacionActiva = false;
    private volatile boolean pausada = false;

    // Datos de la simulación actual
    private String algoritmoIdActual;
    private List<Ruta> rutasActuales = new ArrayList<>();
    private List<MovimientoCamion> movimientosActuales = new ArrayList<>();
    private Map<String, MovimientoCamion> movimientosPorCamion = new HashMap<>();
    private List<Camion> camionesSimulacion = new ArrayList<>();
    private List<Pedido> pedidosOriginales = new ArrayList<>();
    private Mapa mapaSimulacion;

    // Tracking de eventos y entregas
    private List<EstadoSimulacionResponse.EventoReciente> eventosRecientes = new ArrayList<>();
    private Set<String> pedidosEntregados = new HashSet<>();

    // Constantes de simulación
    private static final int SEGUNDOS_INTERVALO = 900; // 15 minutos
    private static final int SEGUNDOS_POR_NODO = 72; // 1km a 50km/h
    private static final int NODOS_POR_INTERVALO = SEGUNDOS_INTERVALO / SEGUNDOS_POR_NODO; // ~12 nodos

    // AÑADIR estos métodos a SimulationStateServiceImpl

    @Override
    public void inicializarSimulacion(String algoritmoId, LocalDateTime fechaInicioPersonalizada) {
        try {
            log.info("Inicializando simulación para algoritmo ID: {} con fecha personalizada: {}",
                    algoritmoId, fechaInicioPersonalizada);

            this.algoritmoIdActual = algoritmoId;

            // Obtener resultados del algoritmo
            var resultado = algoritmoService.obtenerResultados(algoritmoId);
            this.rutasActuales = new ArrayList<>(resultado.getRutas());

            // Obtener datos base
            this.camionesSimulacion = new ArrayList<>(dataRepository.obtenerCamiones());
            this.pedidosOriginales = new ArrayList<>(dataRepository.obtenerPedidos());
            for (Pedido pedido : pedidosOriginales) {
                pedido.setEntregado(false); // Reset para simulación
                pedido.setHoraEntregaReal(null);
            }
            log.info("Estado de pedidos reseteado para simulación en tiempo real");

            this.mapaSimulacion = dataRepository.obtenerMapa();

            // USAR FECHA PERSONALIZADA si se proporciona
            if (fechaInicioPersonalizada != null) {
                this.momentoSimulacionActual = fechaInicioPersonalizada;

                // REGENERAR MOVIMIENTOS con la nueva fecha
                log.info("Regenerando movimientos con fecha personalizada: {}", fechaInicioPersonalizada);
                regenerarMovimientosConNuevaFecha(fechaInicioPersonalizada);
            } else {
                // Usar fecha del algoritmo
                this.momentoSimulacionActual = resultado.getHoraInicio() != null ?
                        resultado.getHoraInicio() : LocalDateTime.now();

                // Cargar movimientos existentes
                cargarMovimientosDesdeCache();
            }

            // Limpiar estado
            this.pedidosEntregados.clear();



            this.simulacionActiva = true;
            this.pausada = false;

            log.info("Simulación inicializada: {} rutas, {} camiones, {} movimientos cargados, momento inicial: {}",
                    rutasActuales.size(), camionesSimulacion.size(), movimientosActuales.size(), momentoSimulacionActual);

        } catch (Exception e) {
            log.error("Error al inicializar simulación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al inicializar simulación", e);
        }
    }

    /**
     * Regenera todos los movimientos con una nueva fecha de inicio
     */
    private void regenerarMovimientosConNuevaFecha(LocalDateTime nuevaFechaInicio) {
        try {
            // Limpiar movimientos actuales
            this.movimientosActuales.clear();
            this.movimientosPorCamion.clear();

            // Configurar bloqueos para el nuevo período DE LA SIMULACIÓN
            LocalDateTime fechaFin = nuevaFechaInicio.plusDays(7);
            mapaSimulacion.filtrarBloqueosParaFecha(nuevaFechaInicio.toLocalDate(), fechaFin.toLocalDate());

            log.info("Bloqueos configurados para simulación: {} - {}",
                    nuevaFechaInicio.toLocalDate(), fechaFin.toLocalDate());

            // Regenerar movimientos usando SimulacionTemporalService
            SimulacionTemporalService simulacionTemporalService = new SimulacionTemporalService(averiaService);

            List<MovimientoCamion> nuevosMovimientos = simulacionTemporalService.generarMovimientosDetallados(
                    rutasActuales, mapaSimulacion, nuevaFechaInicio);

            // Optimizar movimientos
            this.movimientosActuales = simulacionTemporalService.optimizarMovimientosParaVisualizacion(nuevosMovimientos);

            // Organizar en cache por camión
            for (MovimientoCamion movimiento : movimientosActuales) {
                movimientosPorCamion.put(movimiento.getCodigoCamion(), movimiento);

                // Asignar el movimiento a la ruta correspondiente
                for (Ruta ruta : rutasActuales) {
                    if (ruta.getCodigoCamion().equals(movimiento.getCodigoCamion())) {
                        ruta.setMovimientoDetallado(movimiento);
                        break;
                    }
                }
            }

            log.info("Movimientos regenerados con nueva fecha {}: {} movimientos creados",
                    nuevaFechaInicio, movimientosActuales.size());

            log.info("Verificando vínculos pedido-movimiento:");
            int pedidosConEntrega = 0;
            for (MovimientoCamion movimiento : nuevosMovimientos) {
                for (var paso : movimiento.getPasos()) {
                    if (paso.getTipo() == MovimientoCamion.PasoMovimiento.TipoPaso.ENTREGA && paso.getPedidoId() != null) {
                        pedidosConEntrega++;
                    }
                }
            }
            log.info("Total pasos de entrega con pedidoId: {}", pedidosConEntrega);

        } catch (Exception e) {
            log.error("Error al regenerar movimientos con nueva fecha: {}", e.getMessage(), e);
            throw new RuntimeException("Error al regenerar movimientos", e);
        }
    }

    /**
     * Carga movimientos desde el cache del MovimientoController
     * Necesitamos acceder al mismo cache que usa el MovimientoController
     */
    private void cargarMovimientosDesdeCache() {
        // Verificar que las rutas tienen movimientos detallados
        for (Ruta ruta : rutasActuales) {
            if (ruta.getMovimientoDetallado() != null) {
                movimientosActuales.add(ruta.getMovimientoDetallado());
                movimientosPorCamion.put(ruta.getCodigoCamion(), ruta.getMovimientoDetallado());

                log.debug("Movimiento cargado para camión {}: {} pasos",
                        ruta.getCodigoCamion(), ruta.getMovimientoDetallado().getPasos().size());
            } else {
                log.warn("Ruta {} del camión {} no tiene movimiento detallado generado",
                        ruta.getId(), ruta.getCodigoCamion());
            }
        }

        if (movimientosActuales.isEmpty()) {
            log.error("No se encontraron movimientos detallados. ¿Se ejecutó generar-automatico?");
            throw new RuntimeException("No hay movimientos detallados disponibles. Ejecute generar-automatico primero.");
        }
    }

    @Override
    public EstadoSimulacionResponse obtenerEstadoProximos15Min(LocalDateTime momentoSolicitud) {
        if (!simulacionActiva || pausada) {
            throw new IllegalStateException("La simulación no está activa");
        }

        try {
            log.debug("Calculando estado para próximos 15 min desde: {}", momentoSolicitud);

            // Actualizar momento de simulación
            this.momentoSimulacionActual = momentoSolicitud;

            // Procesar entregas que ocurrieron hasta este momento
            procesarEntregasHastaElMomento(momentoSolicitud);

            EstadoSimulacionResponse estado = new EstadoSimulacionResponse();
            estado.setMomentoSimulacion(momentoSolicitud);
            estado.setSimulacionActiva(true);

            // Calcular estado de camiones para próximos 15 min
            estado.setEstadoCamiones(calcularEstadoCamiones15Min(momentoSolicitud));

            // Obtener pedidos pendientes que serán procesados en próximos 15 min
            estado.setPedidosPendientes(obtenerPedidosPendientesProximos15Min(momentoSolicitud));

            // Eventos recientes
            estado.setEventosRecientes(obtenerEventosRecientes(momentoSolicitud));

            // Métricas generales
            estado.setMetricas(calcularMetricasGenerales(momentoSolicitud));

            estado.setBloqueosActivos(obtenerBloqueosActivos(momentoSolicitud));

            log.debug("Estado calculado: {} camiones activos, {} pedidos pendientes para próximos 15 min",
                    estado.getEstadoCamiones().size(), estado.getPedidosPendientes().size());

            return estado;

        } catch (Exception e) {
            log.error("Error al calcular estado de simulación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener estado de simulación", e);
        }
    }

    private void procesarEntregasHastaElMomento(LocalDateTime momento) {
        for (MovimientoCamion movimiento : movimientosActuales) {
            for (var paso : movimiento.getPasos()) {
                if (paso.getTipo() == MovimientoCamion.PasoMovimiento.TipoPaso.ENTREGA &&
                        paso.getPedidoId() != null &&
                        paso.getTiempoLlegada().isBefore(momento) &&
                        !pedidosEntregados.contains(paso.getPedidoId())) {

                    // Marcar pedido como entregado
                    pedidosEntregados.add(paso.getPedidoId());

                    // Actualizar el pedido original
                    for (Pedido pedido : pedidosOriginales) {
                        if (pedido.getId().equals(paso.getPedidoId())) {
                            pedido.setHoraEntregaReal(paso.getTiempoLlegada());
                            pedido.setEntregado(true);

                            log.debug("Pedido {} marcado como entregado en {}",
                                    pedido.getId(), paso.getTiempoLlegada());
                            break;
                        }
                    }

                    // Registrar evento
                    registrarEvento(paso.getTiempoLlegada(), "ENTREGA",
                            "Pedido " + paso.getPedidoId() + " entregado",
                            movimiento.getCodigoCamion(), paso.getUbicacion());
                }
            }
        }
    }

    private List<EstadoSimulacionResponse.EstadoCamionInterval> calcularEstadoCamiones15Min(LocalDateTime momento) {
        List<EstadoSimulacionResponse.EstadoCamionInterval> estados = new ArrayList<>();

        for (Camion camion : camionesSimulacion) {
            EstadoSimulacionResponse.EstadoCamionInterval estado = new EstadoSimulacionResponse.EstadoCamionInterval();
            estado.setCodigo(camion.getCodigo());

            // Obtener movimiento para este camión
            MovimientoCamion movimiento = movimientosPorCamion.get(camion.getCodigo());

            if (movimiento != null) {
                // Obtener posición actual
                var posicion = movimiento.obtenerPosicionEnMomento(momento);
                estado.setPosicionActual(posicion.getUbicacion());
                estado.setProgresoPorcentaje(movimiento.calcularProgreso(momento));
                estado.setEstado(obtenerEstadoMovimiento(movimiento, momento));

                // Calcular nodos que recorrerá en próximos 15 min
                estado.setRutaProximos15Min(calcularNodosProximos15Min(movimiento, momento));

                // Información adicional
                estado.setActividadActual(determinarActividad(movimiento, momento));
                estado.setProximaEntrega(calcularProximaEntrega(movimiento, momento));

            } else {
                // Camión sin ruta activa
                estado.setPosicionActual(camion.getUbicacionActual());
                estado.setEstado(camion.getEstado().toString());
                estado.setRutaProximos15Min(new ArrayList<>());
                estado.setProgresoPorcentaje(0.0);
                estado.setActividadActual("INACTIVO");
            }

            estado.setCombustibleRestante(camion.getNivelCombustibleActual());
            estado.setGlpRestante(camion.getNivelGLPActual());

            estados.add(estado);
        }

        return estados;
    }

    private List<Ubicacion> calcularNodosProximos15Min(MovimientoCamion movimiento, LocalDateTime momento) {
        if (movimiento == null || movimiento.getPasos().isEmpty()) {
            return new ArrayList<>();
        }

        List<Ubicacion> nodosProximos15Min = new ArrayList<>();
        LocalDateTime limiteIntervalo = momento.plusSeconds(SEGUNDOS_INTERVALO);

        // Encontrar posición actual en la secuencia de pasos
        int pasoActualIndex = -1;
        for (int i = 0; i < movimiento.getPasos().size(); i++) {
            var paso = movimiento.getPasos().get(i);
            if (paso.getTiempoLlegada().isAfter(momento)) {
                pasoActualIndex = i;
                break;
            }
        }

        if (pasoActualIndex == -1) {
            return nodosProximos15Min;
        }

        // Agregar nodos que recorrerá en los próximos 15 minutos
        int nodosAgregados = 0;
        for (int i = pasoActualIndex; i < movimiento.getPasos().size() && nodosAgregados < NODOS_POR_INTERVALO; i++) {
            var paso = movimiento.getPasos().get(i);

            if (paso.getTiempoLlegada().isBefore(limiteIntervalo) || paso.getTiempoLlegada().isEqual(limiteIntervalo)) {
                nodosProximos15Min.add(paso.getUbicacion());
                nodosAgregados++;
            } else {
                break;
            }
        }

        log.debug("Camión {} recorrerá {} nodos en próximos 15 min",
                movimiento.getCodigoCamion(), nodosProximos15Min.size());

        return nodosProximos15Min;
    }

    private List<EstadoSimulacionResponse.EstadoPedidoSimulacion> obtenerPedidosPendientesProximos15Min(LocalDateTime momento) {
        LocalDateTime limiteIntervalo = momento.plusSeconds(SEGUNDOS_INTERVALO);
        List<EstadoSimulacionResponse.EstadoPedidoSimulacion> pedidosProximos15Min = new ArrayList<>();

        log.debug("Buscando pedidos pendientes desde {} hasta {}", momento, limiteIntervalo);
        log.debug("Total pedidos originales: {}", pedidosOriginales.size());
        log.debug("Pedidos ya entregados EN SIMULACIÓN: {}", pedidosEntregados.size());

        // Crear mapa de entregas programadas (para pedidos ya asignados)
        Map<String, LocalDateTime> entregasProgramadas = new HashMap<>();
        Map<String, String> camionesAsignados = new HashMap<>();

        for (MovimientoCamion movimiento : movimientosActuales) {
            for (var paso : movimiento.getPasos()) {
                if (paso.getTipo() == MovimientoCamion.PasoMovimiento.TipoPaso.ENTREGA && paso.getPedidoId() != null) {
                    entregasProgramadas.put(paso.getPedidoId(), paso.getTiempoLlegada());
                    camionesAsignados.put(paso.getPedidoId(), movimiento.getCodigoCamion());
                }
            }
        }

        log.debug("Total entregas programadas encontradas: {}", entregasProgramadas.size());

        for (Pedido pedido : pedidosOriginales) {
            // FILTRO 1: Solo pedidos NO entregados en la simulación actual
            if (pedidosEntregados.contains(pedido.getId())) {
                continue; // Este pedido YA fue entregado en la simulación WebSocket
            }

            // FILTRO 2: Solo pedidos que ya están disponibles (su hora de recepción ha pasado)
            if (pedido.getHoraRecepcion().isAfter(momento)) {
                continue; // Este pedido aún no ha sido recibido
            }

            // Ahora clasificar el pedido según su estado
            LocalDateTime horaEntregaEstimada = entregasProgramadas.get(pedido.getId());
            String camionAsignado = camionesAsignados.get(pedido.getId());

            EstadoSimulacionResponse.EstadoPedidoSimulacion estado = new EstadoSimulacionResponse.EstadoPedidoSimulacion();
            estado.setId(pedido.getId());
            estado.setIdCliente(pedido.getIdCliente());
            estado.setUbicacion(pedido.getUbicacion());
            estado.setCantidadGLP(pedido.getCantidadGLP());
            estado.setHoraLimiteEntrega(pedido.getHoraLimiteEntrega());
            estado.setCamionAsignado(camionAsignado);
            estado.setHoraEntregaEstimada(horaEntregaEstimada);

            if (horaEntregaEstimada != null) {
                // CASO 1: Pedido tiene entrega programada
                boolean seraEntregadoEnIntervalo =
                        horaEntregaEstimada.isAfter(momento) &&
                                (horaEntregaEstimada.isBefore(limiteIntervalo) || horaEntregaEstimada.isEqual(limiteIntervalo));

                if (seraEntregadoEnIntervalo) {
                    estado.setEstadoEntrega("SERA_ENTREGADO");
                } else if (horaEntregaEstimada.isAfter(momento)) {
                    estado.setEstadoEntrega("EN_RUTA");
                } else {
                    // La entrega programada ya pasó pero no se ha marcado como entregado
                    estado.setEstadoEntrega("PENDIENTE_VERIFICACION");
                }
            } else {
                // CASO 2: Pedido disponible pero SIN asignación/entrega programada
                // Esto puede pasar si el algoritmo no pudo asignar el pedido
                estado.setEstadoEntrega("PENDIENTE_ASIGNACION");
            }

            // Marcar como urgente si está cerca del límite
            estado.setUrgente(pedido.getHoraLimiteEntrega().isBefore(momento.plusHours(2)));

            // INCLUIR TODOS los pedidos disponibles, no solo los asignados
            pedidosProximos15Min.add(estado);
        }

        log.debug("Pedidos pendientes para próximos 15 min: {}", pedidosProximos15Min.size());

        // Estadísticas adicionales para debugging
        long asignados = pedidosProximos15Min.stream().filter(p -> p.getCamionAsignado() != null).count();
        long noAsignados = pedidosProximos15Min.stream().filter(p -> p.getCamionAsignado() == null).count();
        log.debug("Pedidos asignados: {}, No asignados: {}", asignados, noAsignados);

        return pedidosProximos15Min;
    }

    @Override
    public void generarAveria(String codigoCamion, TipoIncidente tipoIncidente, LocalDateTime momento) {
        // Implementar replanificación...
        log.info("Generando avería: camión {}, tipo {}, momento {}", codigoCamion, tipoIncidente, momento);

        // Por ahora, solo registrar el evento
        registrarEvento(momento, "AVERIA",
                "Avería " + tipoIncidente + " en camión " + codigoCamion,
                codigoCamion, null);
    }

    private void registrarEvento(LocalDateTime momento, String tipo, String descripcion,
                                 String camion, Ubicacion ubicacion) {
        EstadoSimulacionResponse.EventoReciente evento = new EstadoSimulacionResponse.EventoReciente();
        evento.setMomento(momento);
        evento.setTipo(tipo);
        evento.setDescripcion(descripcion);
        evento.setCamionInvolucrado(camion);
        evento.setUbicacion(ubicacion);

        eventosRecientes.add(evento);

        if (eventosRecientes.size() > 50) {
            eventosRecientes.remove(0);
        }
    }

    private List<EstadoSimulacionResponse.EventoReciente> obtenerEventosRecientes(LocalDateTime momento) {
        LocalDateTime limite = momento.minusMinutes(15);
        return eventosRecientes.stream()
                .filter(e -> e.getMomento().isAfter(limite))
                .collect(Collectors.toList());
    }

    private EstadoSimulacionResponse.MetricasGenerales calcularMetricasGenerales(LocalDateTime momento) {
        EstadoSimulacionResponse.MetricasGenerales metricas = new EstadoSimulacionResponse.MetricasGenerales();

        metricas.setPedidosTotales(pedidosOriginales.size());
        metricas.setPedidosEntregados(pedidosEntregados.size());
        metricas.setPedidosPendientes(pedidosOriginales.size() - pedidosEntregados.size());

        long camionesActivos = camionesSimulacion.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE || c.getEstado() == EstadoCamion.EN_RUTA)
                .count();
        metricas.setCamionesActivos((int) camionesActivos);

        long camionesAveriados = camionesSimulacion.stream()
                .filter(c -> c.getEstado() == EstadoCamion.AVERIADO)
                .count();
        metricas.setCamionesAveriados((int) camionesAveriados);

        if (pedidosOriginales.size() > 0) {
            metricas.setPorcentajeCompletado((double) pedidosEntregados.size() / pedidosOriginales.size() * 100);
        }

        metricas.setDistanciaRecorridaTotal(
                rutasActuales.stream().mapToDouble(Ruta::getDistanciaTotal).sum());
        metricas.setCombustibleConsumido(
                rutasActuales.stream().mapToDouble(Ruta::getConsumoCombustible).sum());

        return metricas;
    }

    private String obtenerEstadoMovimiento(MovimientoCamion movimiento, LocalDateTime momento) {
        MovimientoCamion.PasoMovimiento paso = movimiento.obtenerPasoEnMomento(momento);
        if (paso == null) return "PENDIENTE";

        return switch (paso.getTipo()) {
            case INICIO -> "INICIANDO";
            case MOVIMIENTO -> "EN_RUTA";
            case ENTREGA -> "ENTREGANDO";
            case RECARGA_GLP -> "RECARGANDO_GLP";
            case RECARGA_COMBUSTIBLE -> "RECARGANDO_COMBUSTIBLE";
            case FIN -> "COMPLETADO";
            default -> "EN_RUTA";
        };
    }

    private String determinarActividad(MovimientoCamion movimiento, LocalDateTime momento) {
        if (movimiento == null) return "DESCONOCIDO";

        var paso = movimiento.obtenerPasoEnMomento(momento);
        if (paso == null) return "INACTIVO";

        return switch (paso.getTipo()) {
            case INICIO -> "INICIANDO";
            case MOVIMIENTO -> "EN_RUTA";
            case ENTREGA -> "ENTREGANDO";
            case RECARGA_GLP -> "RECARGANDO_GLP";
            case RECARGA_COMBUSTIBLE -> "RECARGANDO_COMBUSTIBLE";
            case FIN -> "COMPLETADO";
            default -> "EN_RUTA";
        };
    }

    private LocalDateTime calcularProximaEntrega(MovimientoCamion movimiento, LocalDateTime momento) {
        for (var paso : movimiento.getPasos()) {
            if (paso.getTipo() == MovimientoCamion.PasoMovimiento.TipoPaso.ENTREGA &&
                    paso.getTiempoLlegada().isAfter(momento)) {
                return paso.getTiempoLlegada();
            }
        }
        return null;
    }

    /**
     * Obtiene los bloqueos activos durante el intervalo de 15 minutos
     */
    private List<EstadoSimulacionResponse.BloqueoActivo> obtenerBloqueosActivos(LocalDateTime momento) {
        LocalDateTime limiteIntervalo = momento.plusSeconds(SEGUNDOS_INTERVALO);
        List<EstadoSimulacionResponse.BloqueoActivo> bloqueosActivos = new ArrayList<>();

        if (mapaSimulacion.getBloqueos() == null || mapaSimulacion.getBloqueos().isEmpty()) {
            return bloqueosActivos;
        }

        for (Bloqueo bloqueo : mapaSimulacion.getBloqueos()) {
            // Verificar si el bloqueo está activo durante el intervalo
            boolean activoEnIntervalo = esBloqueoActivoEnIntervalo(bloqueo, momento, limiteIntervalo);

            if (activoEnIntervalo) {
                EstadoSimulacionResponse.BloqueoActivo bloqueoActivo = new EstadoSimulacionResponse.BloqueoActivo();
                bloqueoActivo.setId(bloqueo.getId());
                bloqueoActivo.setHoraInicio(bloqueo.getHoraInicio());
                bloqueoActivo.setHoraFin(bloqueo.getHoraFin());
                bloqueoActivo.setNodosBloqueados(new ArrayList<>(bloqueo.getNodosBloqueados()));
                bloqueoActivo.setActivoEnIntervalo(true);
                bloqueoActivo.setDescripcion(generarDescripcionBloqueo(bloqueo, momento, limiteIntervalo));

                bloqueosActivos.add(bloqueoActivo);

                log.debug("Bloqueo activo en intervalo: {} con {} nodos bloqueados",
                        bloqueo.getId(), bloqueo.getNodosBloqueados().size());
            }
        }

        log.debug("Bloqueos activos en intervalo {}-{}: {}", momento, limiteIntervalo, bloqueosActivos.size());

        return bloqueosActivos;
    }

    /**
     * Verifica si un bloqueo está activo durante el intervalo
     */
    private boolean esBloqueoActivoEnIntervalo(Bloqueo bloqueo, LocalDateTime inicioIntervalo, LocalDateTime finIntervalo) {
        LocalDateTime bloqueoInicio = bloqueo.getHoraInicio();
        LocalDateTime bloqueoFin = bloqueo.getHoraFin();

        // El bloqueo está activo si hay intersección temporal entre el bloqueo y el intervalo
        return !(bloqueoFin.isBefore(inicioIntervalo) || bloqueoInicio.isAfter(finIntervalo));
    }

    /**
     * Genera descripción detallada del bloqueo
     */
    private String generarDescripcionBloqueo(Bloqueo bloqueo, LocalDateTime inicioIntervalo, LocalDateTime finIntervalo) {
        LocalDateTime bloqueoInicio = bloqueo.getHoraInicio();
        LocalDateTime bloqueoFin = bloqueo.getHoraFin();

        String estado;
        if (bloqueoInicio.isAfter(inicioIntervalo)) {
            estado = "Comenzará durante el intervalo";
        } else if (bloqueoFin.isBefore(finIntervalo)) {
            estado = "Terminará durante el intervalo";
        } else {
            estado = "Activo durante todo el intervalo";
        }

        return String.format("%s (%d nodos bloqueados)", estado, bloqueo.getNodosBloqueados().size());
    }

    @Override
    public void pausarSimulacion() {
        this.pausada = true;
        log.info("Simulación pausada");
    }

    @Override
    public void reanudarSimulacion() {
        this.pausada = false;
        log.info("Simulación reanudada");
    }

    @Override
    public boolean isSimulacionActiva() {
        return simulacionActiva && !pausada;
    }

    @Override
    public void finalizarSimulacion() {
        this.simulacionActiva = false;
        this.pausada = false;
        this.rutasActuales.clear();
        this.movimientosActuales.clear();
        this.movimientosPorCamion.clear();
        this.eventosRecientes.clear();
        this.pedidosEntregados.clear();
        log.info("Simulación finalizada");
    }
}