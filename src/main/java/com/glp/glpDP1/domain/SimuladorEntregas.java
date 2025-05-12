package com.glp.glpDP1.domain;

import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.impl.AveriaService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        return simularEntregas(rutas, momentoInicio, null, false, null);
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
        return simularEntregas(rutas, momentoInicio, mapa, considerarAverias, null);
    }
    
    /**
     * Simula la ejecución de una lista de rutas considerando posibles averías
     * 
     * @param rutas             Lista de rutas generadas por el algoritmo
     * @param momentoInicio     Momento de inicio de la simulación
     * @param mapa              Mapa con información de almacenes
     * @param considerarAverias Si se deben simular averías
     * @param rutasAdicionales  Lista donde se registrarán las rutas adicionales generadas por trasvases
     * @return Las rutas con los tiempos de entrega actualizados
     */
    public List<Ruta> simularEntregas(
            List<Ruta> rutas, 
            LocalDateTime momentoInicio, 
            Mapa mapa,
            boolean considerarAverias,
            List<Ruta> rutasAdicionales) {
        
        for (Ruta ruta : rutas) {
            simularRuta(ruta, momentoInicio, mapa, considerarAverias, rutasAdicionales);
        }
        return rutas;
    }

    /**
     * Simula la ejecución de una ruta específica con manejo detallado de averías
     * 
     * @param ruta              Ruta a simular
     * @param momentoInicio     Momento de inicio
     * @param mapa              Mapa de la ciudad
     * @param considerarAverias Si se deben considerar averías
     * @param rutasAdicionales  Lista donde se registrarán las rutas adicionales generadas por trasvases
     * @return true si la ruta fue interrumpida por avería
     */
    private boolean simularRuta(
            Ruta ruta, 
            LocalDateTime momentoInicio, 
            Mapa mapa, 
            boolean considerarAverias,
            List<Ruta> rutasAdicionales) {
            
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

                    // Simulación de trasvase para tipos TI2 y TI3
                    if ((tipoIncidente == TipoIncidente.TI2 || tipoIncidente == TipoIncidente.TI3) && dataRepository != null) {
                        log.info("  - Se requiere trasvase de carga para continuar entregas");
                        
                        // Procesar trasvase con los camiones reales de la flota
                        List<DetalleTrasvase> trasvases = manejarTrasvase(
                                camion, pedidosPendientes, nodo, momentoActual, ruta, rutasAdicionales);
                        
                        // Registrar trasvases en la ruta
                        for (DetalleTrasvase trasvase : trasvases) {
                            ruta.agregarTrasvase(trasvase);
                        }
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
     * Maneja el trasvase de pedidos durante una avería
     * @param camionAveriado Camión que sufre la avería
     * @param pedidosPendientes Lista de pedidos pendientes a transferir
     * @param ubicacionAveria Ubicación donde ocurre la avería
     * @param momentoActual Momento en que ocurre la avería
     * @param ruta Ruta afectada por la avería
     * @param rutasAdicionales Lista donde se registrarán las rutas generadas por trasvases
     * @return Lista de trasvases realizados
     */
    private List<DetalleTrasvase> manejarTrasvase(
            Camion camionAveriado, 
            List<Pedido> pedidosPendientes, 
            Ubicacion ubicacionAveria,
            LocalDateTime momentoActual,
            Ruta ruta,
            List<Ruta> rutasAdicionales) {
            
        List<DetalleTrasvase> trasvases = new ArrayList<>();
        
        if (dataRepository == null) {
            log.warn("No se puede realizar trasvase: falta repositorio de datos");
            return trasvases;
        }
        
        // Obtener todos los camiones de la flota
        List<Camion> todosLosCamiones = dataRepository.obtenerCamiones();
        
        // Filtrar solo los camiones disponibles (no en mantenimiento, no averiados)
        List<Camion> camionesDisponibles = todosLosCamiones.stream()
                .filter(c -> !c.getCodigo().equals(camionAveriado.getCodigo())) // Excluir el camión averiado
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE || c.getEstado() == EstadoCamion.EN_RUTA)
                .collect(Collectors.toList());
        
        if (camionesDisponibles.isEmpty()) {
            log.warn("No hay camiones disponibles para realizar trasvase");
            return trasvases;
        }
        
        log.info("Camiones disponibles para trasvase: {}", 
                camionesDisponibles.stream()
                        .map(Camion::getCodigo)
                        .collect(Collectors.joining(", ")));
        
        // Ordenar camiones por distancia a la ubicación de la avería
        camionesDisponibles.sort(Comparator.comparingInt(
                c -> c.getUbicacionActual().distanciaA(ubicacionAveria)));
        
        // Tiempo estimado para llegar al punto de avería (para cada camión)
        Map<String, LocalDateTime> tiemposLlegada = new HashMap<>();
        for (Camion camion : camionesDisponibles) {
            int distancia = camion.getUbicacionActual().distanciaA(ubicacionAveria);
            double horasViaje = distancia / camion.getVelocidadPromedio();
            LocalDateTime horaLlegada = momentoActual.plusMinutes((long)(horasViaje * 60));
            tiemposLlegada.put(camion.getCodigo(), horaLlegada);
            
            log.info("  - Camión {} puede llegar en {} min ({}), distancia: {} km",
                    camion.getCodigo(), 
                    (long)(horasViaje * 60),
                    horaLlegada,
                    distancia);
        }
        
        // Asignar pedidos a camiones disponibles
        Map<String, List<Pedido>> pedidosPorCamion = asignarPedidosACamiones(
                pedidosPendientes, camionesDisponibles);
        
        // Para cada camión que recibe pedidos
        for (Map.Entry<String, List<Pedido>> entry : pedidosPorCamion.entrySet()) {
            String codigoCamionDestino = entry.getKey();
            List<Pedido> pedidosTransferidos = entry.getValue();
            
            if (pedidosTransferidos.isEmpty()) continue;
            
            // Camión destino
            Camion camionDestino = camionesDisponibles.stream()
                .filter(c -> c.getCodigo().equals(codigoCamionDestino))
                .findFirst().orElse(null);
            
            if (camionDestino == null) continue;
            
            // Tiempo de llegada del camión destino
            LocalDateTime momentoLlegada = tiemposLlegada.get(codigoCamionDestino);
            
            // Tiempo para realizar el trasvase (15 min por pedido, mínimo 30 min)
            int minutosTotales = Math.max(30, pedidosTransferidos.size() * 15);
            LocalDateTime momentoFinTrasvase = momentoLlegada.plusMinutes(minutosTotales);
            
            // Cantidad total de GLP transferida
            double glpTotal = pedidosTransferidos.stream()
                .mapToDouble(Pedido::getCantidadGLP)
                .sum();
            
            // Crear registro de trasvase
            DetalleTrasvase detalle = new DetalleTrasvase(
                camionAveriado.getCodigo(),
                camionDestino.getCodigo(),
                ubicacionAveria,
                momentoLlegada,
                momentoFinTrasvase,
                new ArrayList<>(pedidosTransferidos),
                glpTotal
            );
            
            trasvases.add(detalle);
            
            // Registrar evento en la ruta
            ruta.registrarEvento(
                EventoRuta.TipoEvento.TRASVASE,
                momentoLlegada,
                ubicacionAveria,
                String.format("Inicio trasvase a camión %s: %d pedidos, %.2f m³", 
                    camionDestino.getCodigo(), 
                    pedidosTransferidos.size(), 
                    glpTotal)
            );
            
            ruta.registrarEvento(
                EventoRuta.TipoEvento.TRASVASE,
                momentoFinTrasvase,
                ubicacionAveria,
                String.format("Fin trasvase a camión %s", camionDestino.getCodigo())
            );
            
            // Log detallado del trasvase
            log.info("DETALLE TRASVASE: {} → {}", camionAveriado.getCodigo(), camionDestino.getCodigo());
            log.info("  - Ubicación: {}", ubicacionAveria);
            log.info("  - Momento inicio: {}", momentoLlegada);
            log.info("  - Momento fin: {}", momentoFinTrasvase);
            log.info("  - Duración: {} minutos", minutosTotales);
            log.info("  - Pedidos: {} unidades", pedidosTransferidos.size());
            log.info("  - Volumen GLP: {:.2f} m³", glpTotal);
            
            // Actualizar estado del camión de destino
            camionDestino.setNivelGLPActual(camionDestino.getNivelGLPActual() + glpTotal);
            camionDestino.setUbicacionActual(ubicacionAveria);  // Se desplaza a la ubicación de la avería
            
            // Crear una nueva ruta para el camión de destino y registrarla si hay una lista para ello
            Ruta nuevaRuta = crearRutaContinuacion(
                camionDestino, 
                pedidosTransferidos, 
                ubicacionAveria, 
                momentoFinTrasvase);
                
            if (rutasAdicionales != null && nuevaRuta != null) {
                rutasAdicionales.add(nuevaRuta);
                log.info("  - Nueva ruta registrada en lista de rutas adicionales: {}", nuevaRuta.getId());
            }
        }
        
        return trasvases;
    }
    
    /**
     * Asigna pedidos a camiones disponibles según su capacidad
     * @return Mapa de código de camión a lista de pedidos asignados
     */
    private Map<String, List<Pedido>> asignarPedidosACamiones(
            List<Pedido> pedidos, List<Camion> camiones) {
        
        Map<String, List<Pedido>> asignacion = new HashMap<>();
        Map<String, Double> capacidadRestante = new HashMap<>();
        
        // Inicializar capacidad restante para cada camión
        for (Camion camion : camiones) {
            capacidadRestante.put(camion.getCodigo(), 
                camion.getCapacidadTanqueGLP() - camion.getNivelGLPActual());
            asignacion.put(camion.getCodigo(), new ArrayList<>());
        }
        
        // Ordenar pedidos por urgencia (deadline más cercano primero)
        List<Pedido> pedidosOrdenados = new ArrayList<>(pedidos);
        pedidosOrdenados.sort(Comparator.comparing(Pedido::getHoraLimiteEntrega));
        
        // Asignar pedidos a camiones
        for (Pedido pedido : pedidosOrdenados) {
            // Buscar el camión con mayor capacidad disponible
            Camion mejorCamion = null;
            double mayorCapacidad = 0;
            
            for (Camion camion : camiones) {
                double capacidad = capacidadRestante.get(camion.getCodigo());
                if (capacidad >= pedido.getCantidadGLP() && capacidad > mayorCapacidad) {
                    mayorCapacidad = capacidad;
                    mejorCamion = camion;
                }
            }
            
            // Asignar al mejor camión encontrado
            if (mejorCamion != null) {
                String codigo = mejorCamion.getCodigo();
                asignacion.get(codigo).add(pedido);
                capacidadRestante.put(codigo, capacidadRestante.get(codigo) - pedido.getCantidadGLP());
                log.info("  - Pedido {} asignado a camión {}: {:.2f} m³ (capacidad restante: {:.2f} m³)",
                        pedido.getId(), codigo, pedido.getCantidadGLP(), capacidadRestante.get(codigo));
            } else {
                log.warn("  - No se pudo transferir pedido {}: {:.2f} m³ (excede capacidad de todos los camiones)",
                        pedido.getId(), pedido.getCantidadGLP());
            }
        }
        
        return asignacion;
    }
    
    /**
     * Crea una nueva ruta para el camión que recibe pedidos en trasvase
     * @return La nueva ruta creada
     */
    private Ruta crearRutaContinuacion(
            Camion camion, 
            List<Pedido> pedidos, 
            Ubicacion origen, 
            LocalDateTime horaInicio) {
        
        // Crear nueva ruta para este camión
        Ruta nuevaRuta = new Ruta(camion.getCodigo(), origen);
        nuevaRuta.setHoraInicio(horaInicio);
        
        // Agregar todos los pedidos recibidos
        for (Pedido pedido : pedidos) {
            nuevaRuta.agregarPedido(pedido);
            pedido.setCamionAsignado(camion.getCodigo());
        }
        
        // Realizar una optimización básica de la secuencia
        nuevaRuta.optimizarSecuencia();
        
        log.info("Nueva ruta creada para camión {} después de trasvase:", camion.getCodigo());
        log.info("  - {} pedidos a entregar", pedidos.size());
        log.info("  - Distancia estimada: {:.2f} km", nuevaRuta.getDistanciaTotal());
        
        // Actualizar estado del camión
        camion.setEstado(EstadoCamion.EN_RUTA);
        
        return nuevaRuta;
    }
}