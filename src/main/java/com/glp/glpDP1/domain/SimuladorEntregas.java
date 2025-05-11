package com.glp.glpDP1.domain;

import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.impl.AveriaService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class SimuladorEntregas {
    private final double velocidadPromedio = 50.0; // km/h
    private final int tiempoDescargaMinutos = 15; // minutos por entrega
    private AveriaService averiaService; // Servicio de averías
    private DataRepository dataRepository; // Repositorio de datos

    public SimuladorEntregas() {
        // Constructor por defecto
    }

    public SimuladorEntregas(AveriaService averiaService) {
        this.averiaService = averiaService;
    }

    public SimuladorEntregas(AveriaService averiaService, DataRepository dataRepository) {
        this.averiaService = averiaService;
        this.dataRepository = dataRepository;
    }
    /**
     * Simula la ejecución de una lista de rutas
     * 
     * @param rutas         Lista de rutas generadas por el algoritmo
     * @param momentoInicio Momento de inicio de la simulación
     * @return Las rutas con los tiempos de entrega actualizados
     */
    public List<Ruta> simularEntregas(List<Ruta> rutas, LocalDateTime momentoInicio) {
        return simularEntregas(rutas, momentoInicio, null, false);
    }

    /**
     * Simula la ejecución de una lista de rutas considerando posibles averías
     * 
     * @param rutas             Lista de rutas generadas por el algoritmo
     * @param momentoInicio     Momento de inicio de la simulación
     * @param mapa              Mapa con información de almacenes
     * @param considerarAverias Si se deben simular averías
     * @return Las rutas con los tiempos de entrega actualizados
     */
    public List<Ruta> simularEntregas(List<Ruta> rutas, LocalDateTime momentoInicio, Mapa mapa,
            boolean considerarAverias) {
        for (Ruta ruta : rutas) {
            simularRuta(ruta, momentoInicio, mapa, considerarAverias);
        }
        return rutas;
    }

    // Método simularRuta mejorado en SimuladorEntregas.java

    /**
     * Simula la ejecución de una ruta específica con manejo detallado de averías
     * 
     * @param ruta              Ruta a simular
     * @param momentoInicio     Momento de inicio
     * @param mapa              Mapa de la ciudad
     * @param considerarAverias Si se deben considerar averías
     * @return true si la ruta fue interrumpida por avería
     */
    private boolean simularRuta(Ruta ruta, LocalDateTime momentoInicio, Mapa mapa, boolean considerarAverias) {
        // Establecer hora de inicio
        ruta.setHoraInicio(momentoInicio);
        log.info("------------------------------------------------------------------");
        log.info("Simulando ruta {} para camión {} con {} pedidos, inicio: {}",
                ruta.getId(), ruta.getCodigoCamion(), ruta.getPedidosAsignados().size(), momentoInicio);

        // Obtener ubicaciones
        Ubicacion ubicacionActual = ruta.getOrigen();
        List<Ubicacion> secuencia = ruta.getSecuenciaNodos();

        // Verificar si la ruta tiene nodos
        if (secuencia.isEmpty()) {
            log.warn("Ruta {} no tiene nodos, no se puede simular", ruta.getId());
            return false;
        }

        // Mapa para asociar ubicaciones con pedidos
        Map<Ubicacion, List<Pedido>> pedidosPorUbicacion = new HashMap<>();

        // Agrupar pedidos por ubicación
        for (Pedido pedido : ruta.getPedidosAsignados()) {
            Ubicacion ubicacion = pedido.getUbicacion();
            if (!pedidosPorUbicacion.containsKey(ubicacion)) {
                pedidosPorUbicacion.put(ubicacion, new ArrayList<>());
            }
            pedidosPorUbicacion.get(ubicacion).add(pedido);
        }

        // Obtener el camión asignado a la ruta
        Camion camion = null;
        if (considerarAverias) {
            String codigoCamion = ruta.getCodigoCamion();
            if (dataRepository != null) {
                camion = dataRepository.buscarCamion(codigoCamion);
            }

            if (camion == null) {
                // Si no se puede obtener del repositorio, crear uno temporal
                camion = new Camion(codigoCamion, null, ruta.getOrigen());
                log.warn("Usando camión temporal para simulación: {}", codigoCamion);
            }
        }

        // Momento actual de la simulación
        LocalDateTime momentoActual = momentoInicio;

        // Verificar si hay avería programada
        boolean hayAveriaProgamada = false;
        int indicePuntoAveria = -1;
        TipoIncidente tipoIncidente = null;

        if (considerarAverias && averiaService != null && camion != null) {
            // Verificar si hay avería programada para este turno
            Turno turnoActual = Turno.obtenerTurnoPorHora(momentoActual.getHour());
            hayAveriaProgamada = averiaService.tieneProgramadaAveria(camion.getCodigo(), momentoActual);

            if (hayAveriaProgamada) {
                log.info("AVERÍA PROGRAMADA: Camión {} en turno {}", camion.getCodigo(), turnoActual);

                // Determinar punto de avería (entre 5% y 35% de la ruta)
                if (secuencia.size() >= 3) {
                    int minIndex = Math.max(1, (int) (secuencia.size() * 0.05));
                    int maxIndex = Math.min(secuencia.size() - 1, (int) (secuencia.size() * 0.35));
                    indicePuntoAveria = new Random().nextInt(maxIndex - minIndex + 1) + minIndex;
                } else {
                    // Si hay pocos nodos, usar el primer nodo
                    indicePuntoAveria = 0;
                }

                // Obtener tipo de incidente
                tipoIncidente = averiaService.obtenerIncidenteProgramado(camion.getCodigo(), turnoActual);

                log.info("  - Ruta tiene {} nodos, avería ocurrirá en nodo {} ({}% de la ruta)",
                        secuencia.size(),
                        indicePuntoAveria,
                        Math.round((double) indicePuntoAveria / secuencia.size() * 100));
                log.info("  - Tipo de incidente: {}", tipoIncidente);

                // Registrar que esta avería ya fue procesada
                averiaService.registrarAveriaOcurrida(camion.getCodigo(), momentoActual);
            }
        }

        // Lista para almacenar pedidos entregados
        List<Pedido> pedidosEntregados = new ArrayList<>();

        // Recorrer la secuencia de nodos
        for (int i = 0; i < secuencia.size(); i++) {
            Ubicacion nodo = secuencia.get(i);
            log.debug("Camión {} avanzando al nodo {}/{}: {}",
                    ruta.getCodigoCamion(), i + 1, secuencia.size(), nodo);

            // Calcular distancia y tiempo hasta el siguiente nodo
            int distancia = ubicacionActual.distanciaA(nodo);
            double horasViaje = distancia / velocidadPromedio;
            long minutosViaje = Math.round(horasViaje * 60);

            // Avanzar el tiempo
            momentoActual = momentoActual.plusMinutes(minutosViaje);
            log.debug("  - Llegada a nodo {}: {} (tras {} min de viaje)",
                    nodo, momentoActual, minutosViaje);

            // Verificar si estamos en el punto donde ocurre la avería
            if (i == indicePuntoAveria && hayAveriaProgamada && camion != null && tipoIncidente != null) {
                // Registrar la avería en el camión
                camion.registrarAveria(tipoIncidente, momentoActual);

                // Registrar evento de avería en la ruta
                ruta.registrarEvento(
                        EventoRuta.TipoEvento.AVERIA,
                        momentoActual,
                        nodo,
                        "Avería tipo " + tipoIncidente + " en " + camion.getCodigo() +
                                " (nodo " + nodo + ", hora " + momentoActual + ")");

                log.info("AVERÍA OCURRIDA en ruta {}: camión {} - tipo {} - ubicación {} - hora {}",
                        ruta.getId(), camion.getCodigo(), tipoIncidente, nodo, momentoActual);

                // Calcular carga restante para posible trasvase
                double cargaRestante = 0;
                List<Pedido> pedidosPendientes = new ArrayList<>();

                for (Ubicacion ubicacionFutura : secuencia.subList(i, secuencia.size())) {
                    if (pedidosPorUbicacion.containsKey(ubicacionFutura)) {
                        for (Pedido pedido : pedidosPorUbicacion.get(ubicacionFutura)) {
                            if (!pedidosEntregados.contains(pedido)) {
                                cargaRestante += pedido.getCantidadGLP();
                                pedidosPendientes.add(pedido);
                            }
                        }
                    }
                }

                if (!pedidosPendientes.isEmpty()) {
                    log.info("  - Carga pendiente para entrega: {:.2f} m³ ({} pedidos)",
                            cargaRestante, pedidosPendientes.size());

                    // Simulación simplificada de trasvase: marcar un porcentaje de pedidos como no
                    // entregables
                    if (tipoIncidente == TipoIncidente.TI2 || tipoIncidente == TipoIncidente.TI3) {
                        log.info("  - Se requiere trasvase de carga para continuar entregas");

                        // Simular que el 50% de los pedidos se trasvasan (simplificación)
                        int pedidosTrasvase = pedidosPendientes.size() / 2;
                        log.info("  - Simulación: {} de {} pedidos podrán ser entregados por otras unidades",
                                pedidosTrasvase, pedidosPendientes.size());
                    }
                }

                // Aplicar efectos según tipo de avería
                if (tipoIncidente == TipoIncidente.TI1) {
                    // 2h inmovilizado, luego continúa
                    momentoActual = momentoActual.plusHours(2);
                    log.info("  - Camión {} inmovilizado 2h, continuará en {}", camion.getCodigo(), momentoActual);
                } else if (tipoIncidente == TipoIncidente.TI2) {
                    // 2h inmovilizado, luego indisponible por un turno
                    momentoActual = momentoActual.plusHours(2);
                    log.info("  - Camión {} inmovilizado 2h, interrumpe ruta (estará en taller)", camion.getCodigo());

                    // Marcar la ruta como interrumpida
                    ruta.setCancelada(true);

                    // Si hay mapa, calcular regreso al almacén más cercano
                    if (mapa != null) {
                        Almacen almacenCercano = mapa.obtenerAlmacenMasCercano(nodo);

                        // Calcular tiempo de regreso
                        int distanciaRegreso = nodo.distanciaA(almacenCercano.getUbicacion());
                        double horasRegreso = distanciaRegreso / velocidadPromedio;
                        momentoActual = momentoActual.plusMinutes((long) (horasRegreso * 60));

                        log.info("  - Camión {} regresa al almacén {} después de inmovilización, llegará: {}",
                                camion.getCodigo(), almacenCercano.getId(), momentoActual);

                        // Registrar evento de regreso
                        ruta.registrarEvento(
                                EventoRuta.TipoEvento.FIN,
                                momentoActual,
                                almacenCercano.getUbicacion(),
                                "Ruta interrumpida por avería TI2, regreso a almacén " + almacenCercano.getId());
                    }

                    // Finalizar simulación de esta ruta
                    ruta.setHoraFinReal(momentoActual);
                    log.info("  - Ruta {} finalizada prematuramente por avería tipo 2", ruta.getId());
                    return true;
                } else if (tipoIncidente == TipoIncidente.TI3) {
                    // 4h inmovilizado, luego indisponible por 3 días
                    momentoActual = momentoActual.plusHours(4);
                    log.info("  - Camión {} inmovilizado 4h, interrumpe ruta (estará en taller 3 días)",
                            camion.getCodigo());

                    // Marcar la ruta como interrumpida
                    ruta.setCancelada(true);

                    // Si hay mapa, calcular regreso al almacén más cercano
                    if (mapa != null) {
                        Almacen almacenCercano = mapa.obtenerAlmacenMasCercano(nodo);

                        // Calcular tiempo de regreso
                        int distanciaRegreso = nodo.distanciaA(almacenCercano.getUbicacion());
                        double horasRegreso = distanciaRegreso / velocidadPromedio;
                        momentoActual = momentoActual.plusMinutes((long) (horasRegreso * 60));

                        log.info("  - Camión {} regresa al almacén {} después de inmovilización, llegará: {}",
                                camion.getCodigo(), almacenCercano.getId(), momentoActual);

                        // Registrar evento de regreso
                        ruta.registrarEvento(
                                EventoRuta.TipoEvento.FIN,
                                momentoActual,
                                almacenCercano.getUbicacion(),
                                "Ruta interrumpida por avería TI3, regreso a almacén " + almacenCercano.getId());
                    }

                    // Finalizar simulación de esta ruta
                    ruta.setHoraFinReal(momentoActual);
                    log.info("  - Ruta {} finalizada prematuramente por avería tipo 3", ruta.getId());
                    return true;
                }
            }

            // Verificar si hay pedidos en este nodo
            if (pedidosPorUbicacion.containsKey(nodo)) {
                List<Pedido> pedidosEnNodo = pedidosPorUbicacion.get(nodo);
                log.debug("  - {} pedidos para entregar en nodo {}", pedidosEnNodo.size(), nodo);

                for (Pedido pedido : pedidosEnNodo) {
                    // Programar hora de entrega
                    pedido.setHoraEntregaProgramada(momentoActual);
                    pedido.setHoraEntregaReal(momentoActual);
                    pedido.setEntregado(true);
                    pedidosEntregados.add(pedido);

                    // Registrar evento de entrega
                    ruta.registrarEvento(
                            EventoRuta.TipoEvento.ENTREGA,
                            momentoActual,
                            nodo,
                            "Entrega completada para cliente " + pedido.getIdCliente() +
                                    " (" + pedido.getCantidadGLP() + "m³)");

                    log.debug("  - Entrega a cliente {} completada: {} m³",
                            pedido.getIdCliente(), pedido.getCantidadGLP());

                    // Agregar tiempo de descarga
                    momentoActual = momentoActual.plusMinutes(tiempoDescargaMinutos);
                }
            }

            // Actualizar ubicación actual
            ubicacionActual = nodo;
        }

        // Calcular regreso al destino final (si no hubo interrupción)
        if (ruta.getDestino() != null) {
            int distanciaRegreso = ubicacionActual.distanciaA(ruta.getDestino());
            double horasRegreso = distanciaRegreso / velocidadPromedio;
            momentoActual = momentoActual.plusMinutes((long) (horasRegreso * 60));

            log.info("Camión {} regresa al destino. Llegada estimada: {}",
                    ruta.getCodigoCamion(), momentoActual);
        }

        // Finalizar la ruta
        ruta.setHoraFinEstimada(momentoActual);
        ruta.setHoraFinReal(momentoActual);
        ruta.setCompletada(true);

        // Registrar evento de finalización
        ruta.registrarEvento(
                EventoRuta.TipoEvento.FIN,
                momentoActual,
                ruta.getDestino() != null ? ruta.getDestino() : ubicacionActual,
                "Ruta completada con éxito: " + pedidosEntregados.size() + " pedidos entregados");

        log.info("Ruta {} completada. Pedidos entregados: {}/{}, hora fin: {}",
                ruta.getId(), pedidosEntregados.size(), ruta.getPedidosAsignados().size(), momentoActual);

        return false; // La ruta no fue interrumpida
    }

    /**
     * Método auxiliar para obtener los camiones de un almacén
     * En un sistema real, esto se haría a través de un repositorio o servicio
     */
    private List<Camion> getCamionesDelAlmacen(Mapa mapa, Almacen almacen) {
        // Esta es una implementación simplificada para fines de simulación
        List<Camion> camiones = new ArrayList<>();

        // En un sistema real, se consultaría la base de datos o un servicio
        // para obtener los camiones asignados a este almacén

        return camiones;
    }
}