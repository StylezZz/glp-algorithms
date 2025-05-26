package com.glp.glpDP1.domain;

import com.glp.glpDP1.services.impl.MonitoreoService;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Representa una ruta para un camión, con una secuencia de nodos, pedidos asignados
 * y movimientos detallados paso a paso
 */
@Getter @Setter
public class Ruta {
    private final String id;
    private final String codigoCamion;

    private List<Ubicacion> secuenciaNodos; // Todos los nodos del camino
    private List<Ubicacion> secuenciaParadas; // Solo nodos de parada (inicio, fin, entregas, recargas)

    private List<Pedido> pedidosAsignados;
    private Ubicacion origen;
    private Ubicacion destino;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinEstimada;
    private LocalDateTime horaFinReal;
    private double distanciaTotal;
    private double consumoCombustible;
    private boolean completada;
    private boolean cancelada;
    private List<EventoRuta> eventos;
    private boolean requiereReabastecimiento;
    private boolean factibilidadEncontrada;

    // NUEVO: Movimiento detallado
    private MovimientoCamion movimientoDetallado;
    private boolean movimientoGenerado;

    private MonitoreoService monitoreoService;

    public Ruta(String codigoCamion, Ubicacion origen) {
        this.id = UUID.randomUUID().toString();
        this.codigoCamion = codigoCamion;
        this.origen = origen;
        this.secuenciaNodos = new ArrayList<>();
        this.secuenciaParadas = new ArrayList<>();
        this.pedidosAsignados = new ArrayList<>();
        this.distanciaTotal = 0;
        this.consumoCombustible = 0;
        this.completada = false;
        this.cancelada = false;
        this.eventos = new ArrayList<>();
        this.requiereReabastecimiento = false;
        this.movimientoGenerado = false;
    }

    /**
     * Genera el movimiento detallado para esta ruta
     */
    public void generarMovimientoDetallado(Mapa mapa, LocalDateTime horaInicio) {
        if (movimientoGenerado || secuenciaNodos.isEmpty()) {
            return;
        }

        this.horaInicio = horaInicio;
        movimientoDetallado = new MovimientoCamion(codigoCamion, id);
        movimientoDetallado.setHoraInicio(horaInicio);

        LocalDateTime tiempoActual = horaInicio;
        Ubicacion ubicacionActual = origen;

        // Agregar paso inicial
        movimientoDetallado.agregarPaso(new MovimientoCamion.PasoMovimiento(
                ubicacionActual,
                tiempoActual,
                MovimientoCamion.PasoMovimiento.TipoPaso.INICIO,
                "Inicio de ruta para camión " + codigoCamion
        ));

        // Procesar cada nodo de la secuencia
        for (int i = 0; i < secuenciaNodos.size(); i++) {
            Ubicacion siguienteNodo = secuenciaNodos.get(i);

            // Generar ruta detallada entre ubicación actual y siguiente nodo
            List<Ubicacion> rutaDetallada = mapa.encontrarRuta(ubicacionActual, siguienteNodo, tiempoActual);

            if (!rutaDetallada.isEmpty()) {
                // Agregar cada paso del movimiento
                for (int j = 1; j < rutaDetallada.size(); j++) { // Empezar en 1 para evitar duplicar posición actual
                    Ubicacion nodo = rutaDetallada.get(j);

                    // Calcular tiempo de llegada (velocidad promedio 50 km/h)
                    Ubicacion nodoAnterior = rutaDetallada.get(j - 1);
                    int distancia = nodoAnterior.distanciaA(nodo);
                    long minutosViaje = Math.round((double) distancia / 50.0 * 60);
                    tiempoActual = tiempoActual.plusMinutes(minutosViaje);

                    movimientoDetallado.agregarPaso(new MovimientoCamion.PasoMovimiento(
                            nodo,
                            tiempoActual,
                            MovimientoCamion.PasoMovimiento.TipoPaso.MOVIMIENTO,
                            String.format("Movimiento a (%d,%d)", nodo.getX(), nodo.getY())
                    ));
                }

                ubicacionActual = siguienteNodo;
            }

            // Verificar si hay una entrega en este nodo
            Pedido pedidoEnNodo = encontrarPedidoEnUbicacion(siguienteNodo);
            if (pedidoEnNodo != null) {
                // Agregar tiempo de entrega
                tiempoActual = tiempoActual.plusMinutes(15);

                movimientoDetallado.agregarPaso(new MovimientoCamion.PasoMovimiento(
                        siguienteNodo,
                        tiempoActual,
                        MovimientoCamion.PasoMovimiento.TipoPaso.ENTREGA,
                        "Entrega para cliente " + pedidoEnNodo.getIdCliente(),
                        pedidoEnNodo.getId(),
                        15.0
                ));

                // Registrar evento de entrega
                registrarEvento(EventoRuta.TipoEvento.ENTREGA, tiempoActual, siguienteNodo,
                        "Entrega programada para cliente " + pedidoEnNodo.getIdCliente());
            }
        }

        // Agregar regreso al destino final
        if (destino != null && !ubicacionActual.equals(destino)) {
            List<Ubicacion> rutaRegreso = mapa.encontrarRuta(ubicacionActual, destino, tiempoActual);

            for (int j = 1; j < rutaRegreso.size(); j++) {
                Ubicacion nodo = rutaRegreso.get(j);
                Ubicacion nodoAnterior = rutaRegreso.get(j - 1);
                int distancia = nodoAnterior.distanciaA(nodo);
                long minutosViaje = Math.round((double) distancia / 50.0 * 60);
                tiempoActual = tiempoActual.plusMinutes(minutosViaje);

                movimientoDetallado.agregarPaso(new MovimientoCamion.PasoMovimiento(
                        nodo,
                        tiempoActual,
                        MovimientoCamion.PasoMovimiento.TipoPaso.MOVIMIENTO,
                        String.format("Regreso a (%d,%d)", nodo.getX(), nodo.getY())
                ));
            }
        }

        // Agregar paso final
        movimientoDetallado.agregarPaso(new MovimientoCamion.PasoMovimiento(
                destino != null ? destino : ubicacionActual,
                tiempoActual,
                MovimientoCamion.PasoMovimiento.TipoPaso.FIN,
                "Fin de ruta para camión " + codigoCamion
        ));

        movimientoDetallado.setHoraFinEstimada(tiempoActual);
        movimientoDetallado.setEstado(MovimientoCamion.EstadoMovimiento.PENDIENTE);
        this.horaFinEstimada = tiempoActual;
        this.movimientoGenerado = true;

        // Registrar evento de finalización estimada
        registrarEvento(EventoRuta.TipoEvento.FIN, tiempoActual,
                destino != null ? destino : ubicacionActual,
                "Fin estimado de ruta");
    }

    /**
     * Obtiene la posición del camión en un momento específico
     */
    public MovimientoCamion.PosicionCamion obtenerPosicionEnMomento(LocalDateTime momento) {
        if (!movimientoGenerado || movimientoDetallado == null) {
            return new MovimientoCamion.PosicionCamion(
                    origen, 0.0, MovimientoCamion.EstadoMovimiento.PENDIENTE);
        }

        return movimientoDetallado.obtenerPosicionEnMomento(momento);
    }

    /**
     * Obtiene el progreso de la ruta en un momento específico (0-100%)
     */
    public double obtenerProgresoEnMomento(LocalDateTime momento) {
        if (!movimientoGenerado || movimientoDetallado == null) {
            return 0.0;
        }

        return movimientoDetallado.calcularProgreso(momento);
    }

    /**
     * Obtiene todos los timestamps importantes de la ruta para animación
     */
    public List<LocalDateTime> obtenerTimestampsAnimacion() {
        if (!movimientoGenerado || movimientoDetallado == null) {
            return new ArrayList<>();
        }

        return movimientoDetallado.getPasos().stream()
                .map(MovimientoCamion.PasoMovimiento::getTiempoLlegada)
                .sorted()
                .toList();
    }

    /**
     * Obtiene el estado del camión en un momento específico
     */
    public String obtenerEstadoEnMomento(LocalDateTime momento) {
        if (!movimientoGenerado || movimientoDetallado == null) {
            return "PENDIENTE";
        }

        MovimientoCamion.PasoMovimiento paso = movimientoDetallado.obtenerPasoEnMomento(momento);
        if (paso == null) {
            return "PENDIENTE";
        }

        return switch (paso.getTipo()) {
            case INICIO -> "INICIANDO";
            case MOVIMIENTO -> "EN_RUTA";
            case ENTREGA -> "ENTREGANDO";
            case RECARGA_GLP -> "RECARGANDO_GLP";
            case RECARGA_COMBUSTIBLE -> "RECARGANDO_COMBUSTIBLE";
            case AVERIA -> "AVERIADO";
            case FIN -> "COMPLETADO";
            default -> "EN_RUTA";
        };
    }

    // Método auxiliar para encontrar pedido en ubicación
    private Pedido encontrarPedidoEnUbicacion(Ubicacion ubicacion) {
        return pedidosAsignados.stream()
                .filter(p -> p.getUbicacion().equals(ubicacion))
                .findFirst()
                .orElse(null);
    }

    /**
     * Añade un pedido a la ruta
     * @param pedido Pedido a añadir
     */
    public void agregarPedido(Pedido pedido) {
        if (!pedidosAsignados.contains(pedido)) {
            pedidosAsignados.add(pedido);
            pedido.setCamionAsignado(codigoCamion);

            // Añadir el nodo del pedido a los puntos de PARADA
            if (!secuenciaParadas.contains(pedido.getUbicacion())) {
                secuenciaParadas.add(pedido.getUbicacion());
            }

            // Marcar que necesita regenerar movimiento
            movimientoGenerado = false;
        }
    }

    /**
     * Elimina un pedido de la ruta
     * @param pedidoId ID del pedido a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean eliminarPedido(String pedidoId) {
        Pedido pedidoAEliminar = null;
        for (Pedido p : pedidosAsignados) {
            if (p.getId().equals(pedidoId)) {
                pedidoAEliminar = p;
                break;
            }
        }

        if (pedidoAEliminar != null) {
            pedidosAsignados.remove(pedidoAEliminar);
            pedidoAEliminar.setCamionAsignado(null);

            // Verificar si hay que eliminar de PARADAS
            boolean hayOtroPedidoEnMismoNodo = false;
            Ubicacion ubicacionPedido = pedidoAEliminar.getUbicacion();

            for (Pedido p : pedidosAsignados) {
                if (p.getUbicacion().equals(ubicacionPedido)) {
                    hayOtroPedidoEnMismoNodo = true;
                    break;
                }
            }

            if (!hayOtroPedidoEnMismoNodo) {
                secuenciaParadas.remove(ubicacionPedido);
            }

            // Marcar que necesita regenerar movimiento
            movimientoGenerado = false;
            return true;
        }

        return false;
    }

    /**
     * Registra la entrega de un pedido
     * @param pedidoId ID del pedido entregado
     * @param momento Momento de la entrega
     * @return true si se registró correctamente
     */
    public boolean registrarEntrega(String pedidoId, LocalDateTime momento) {
        for (Pedido pedido : pedidosAsignados) {
            if (pedido.getId().equals(pedidoId)) {
                pedido.setHoraEntregaReal(momento);
                registrarEvento(EventoRuta.TipoEvento.ENTREGA, momento, pedido.getUbicacion(),
                        "Entrega completada para cliente " + pedido.getIdCliente());
                return true;
            }
        }
        return false;
    }

    /**
     * Calcula la distancia total de la ruta
     */
    private void calcularDistanciaTotal() {
        if (secuenciaNodos.isEmpty()) {
            distanciaTotal = 0;
            return;
        }

        distanciaTotal = origen.distanciaA(secuenciaNodos.get(0));

        for (int i = 0; i < secuenciaNodos.size() - 1; i++) {
            distanciaTotal += secuenciaNodos.get(i).distanciaA(secuenciaNodos.get(i + 1));
        }

        if (destino != null && !secuenciaNodos.isEmpty()) {
            distanciaTotal += secuenciaNodos.get(secuenciaNodos.size() - 1).distanciaA(destino);
        }
    }

    /**
     * Calcula el consumo de combustible para un camión específico
     * @param camion Camión para el cálculo
     * @return Consumo estimado en galones
     */
    public double calcularConsumoCombustible(Camion camion) {
        double consumo = 0;
        double pesoActual = camion.calcularPesoTotal();

        if (secuenciaNodos.isEmpty()) {
            this.consumoCombustible = 0;
            return 0;
        }

        // Tramo inicial: origen a primer nodo
        consumo += (origen.distanciaA(secuenciaNodos.get(0)) * pesoActual) / 180.0;

        // Tramos intermedios
        for (int i = 0; i < secuenciaNodos.size() - 1; i++) {
            Ubicacion actual = secuenciaNodos.get(i);
            Ubicacion siguiente = secuenciaNodos.get(i + 1);

            // Reducir peso después de cada entrega (aproximación)
            if (i < pedidosAsignados.size()) {
                pesoActual -= pedidosAsignados.get(i).getCantidadGLP() * 0.5; // 0.5 ton/m3
                if (pesoActual < camion.getPesoTara()) {
                    pesoActual = camion.getPesoTara();
                }
            }

            consumo += (actual.distanciaA(siguiente) * pesoActual) / 180.0;
        }

        // Tramo final: último nodo a destino
        if (destino != null && !secuenciaNodos.isEmpty()) {
            consumo += (secuenciaNodos.get(secuenciaNodos.size() - 1).distanciaA(destino) * camion.getPesoTara()) / 180.0;
        }

        // Actualizar el valor del consumo en el objeto
        this.consumoCombustible = consumo;
        return consumo;
    }

    /**
     * Registra un evento en la ruta
     * @param tipo Tipo de evento
     * @param momento Momento del evento
     * @param ubicacion Ubicación del evento
     * @param descripcion Descripción del evento
     */
    public void registrarEvento(EventoRuta.TipoEvento tipo, LocalDateTime momento,
                                Ubicacion ubicacion, String descripcion) {
        EventoRuta evento = new EventoRuta(tipo, momento, ubicacion, descripcion);
        eventos.add(evento);
    }

    /**
     * Optimiza la secuencia de nodos para minimizar la distancia total
     */
    public void optimizarSecuencia() {
        // Optimizar la secuencia de PARADAS
        if (secuenciaParadas.size() <= 1) {
            return;
        }

        List<Ubicacion> nuevaSecuenciaParadas = new ArrayList<>();
        List<Ubicacion> pendientes = new ArrayList<>(secuenciaParadas);

        Ubicacion actual = origen;
        while (!pendientes.isEmpty()) {
            Ubicacion masProxima = null;
            int distanciaMinima = Integer.MAX_VALUE;

            for (Ubicacion u : pendientes) {
                int distancia = actual.distanciaA(u);
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    masProxima = u;
                }
            }

            nuevaSecuenciaParadas.add(masProxima);
            pendientes.remove(masProxima);
            actual = masProxima;
        }

        this.secuenciaParadas = nuevaSecuenciaParadas;

        // También actualizar la secuencia completa de nodos
        this.secuenciaNodos = new ArrayList<>(secuenciaParadas);
        this.secuenciaNodos.add(0, origen);

        calcularDistanciaTotal();

        // Marcar que necesita regenerar movimiento
        movimientoGenerado = false;
    }

    public void optimizarConRecargas(Mapa mapa, Camion camion) {
        // Primero optimizar la secuencia base
        optimizarSecuencia();

        List<Ubicacion> nuevaSecuenciaParadas = new ArrayList<>();
        List<Almacen> almacenes = mapa.getAlmacenes();

        // Obtener almacén central (para priorizar)
        Almacen almacenCentral = mapa.obtenerAlmacenCentral();

        Ubicacion actual = origen;
        double combustibleActual = camion.getNivelCombustibleActual();

        double glpTotal = pedidosAsignados.stream().mapToDouble(Pedido::getCantidadGLP).sum();

        boolean necesitaRecargaGLP = glpTotal > camion.getCapacidadTanqueGLP();

        // Trabajar con secuenciaParadas
        if(necesitaRecargaGLP && !secuenciaParadas.contains(almacenCentral.getUbicacion())){
            int puntoMedio = secuenciaParadas.size() / 2;
            if(puntoMedio > 0 && puntoMedio < secuenciaParadas.size()){
                secuenciaParadas.add(puntoMedio, almacenCentral.getUbicacion());
                // Registrar el evento de recarga de GLP
                registrarEvento(
                        EventoRuta.TipoEvento.RECARGA_GLP,
                        LocalDateTime.now(),
                        almacenCentral.getUbicacion(),
                        "Recarga de GLP en almacén central"
                );
            }
        }

        for (Ubicacion siguiente : secuenciaParadas) {
            int distanciaAlSiguiente = actual.distanciaA(siguiente);

            // Verificar si tenemos suficiente combustible
            Almacen almacenMasCercanoASiguiente = mapa.obtenerAlmacenMasCercano(siguiente);
            int distanciaAlAlmacenDesdeSiguiente = siguiente.distanciaA(almacenMasCercanoASiguiente.getUbicacion());

            double consumoHastaSiguiente = camion.calcularConsumoCombustible(distanciaAlSiguiente);
            double consumoDeRegresoAlAlmacen = camion.calcularConsumoCombustible(distanciaAlAlmacenDesdeSiguiente);
            double consumoTotal = consumoHastaSiguiente + consumoDeRegresoAlAlmacen;

            // Si no hay suficiente combustible, buscar recarga
            if (combustibleActual < consumoTotal) {
                // Decidir qué almacén usar para recargar
                Almacen almacenParaRecargar;

                // Si la ruta tiene alta demanda de GLP o contiene pedidos urgentes, priorizar almacén central
                boolean hayPedidosUrgentes = pedidosAsignados.stream()
                        .anyMatch(p -> p.getTiempoLimiteEntrega().toHours() < 8);

                if (necesitaRecargaGLP || hayPedidosUrgentes) {
                    almacenParaRecargar = almacenCentral;
                } else {
                    almacenParaRecargar = mapa.obtenerAlmacenMasCercano(actual);
                }

                // Añadir desvío al almacén para recargar
                nuevaSecuenciaParadas.add(almacenParaRecargar.getUbicacion());

                // Registrar el evento de recarga
                registrarEvento(
                        EventoRuta.TipoEvento.RECARGA_COMBUSTIBLE,
                        LocalDateTime.now(),
                        almacenParaRecargar.getUbicacion(),
                        "Recarga de combustible en " + almacenParaRecargar.getId()
                );

                // Actualizar el combustible (tanque lleno)
                combustibleActual = camion.getCapacidadTanqueCombustible();

                // Ahora vamos desde el almacén al siguiente punto
                actual = almacenParaRecargar.getUbicacion();
                distanciaAlSiguiente = actual.distanciaA(siguiente);
                consumoHastaSiguiente = camion.calcularConsumoCombustible(distanciaAlSiguiente);
            }

            // Añadir el punto a la secuencia de PARADAS
            nuevaSecuenciaParadas.add(siguiente);

            // Actualizar combustible y posición actual
            combustibleActual -= consumoHastaSiguiente;
            actual = siguiente;
        }

        // Actualizar la secuencia de paradas con las recargas
        this.secuenciaParadas = nuevaSecuenciaParadas;
        calcularDistanciaTotal();

        // Marcar que necesita regenerar movimiento
        movimientoGenerado = false;
    }

    public void actualizarEstadoMonitoreo(Ubicacion posicionActual, LocalDateTime momento) {
        if (monitoreoService == null) return;

        MonitoreoService.EstadoRuta estado = new MonitoreoService.EstadoRuta();
        estado.setIdRuta(id);
        estado.setCodigoCamion(codigoCamion);
        estado.setNodosRecorridos(new ArrayList<>(secuenciaNodos));
        estado.setPosicionActual(posicionActual);
        estado.setPedidosEntregados(calcularPedidosEntregados());
        estado.setPedidosTotales(pedidosAsignados.size());
        estado.setConsumoCombustible(consumoCombustible);
        estado.setDistanciaRecorrida(distanciaTotal);
        estado.setUltimaActualizacion(momento);
        estado.setCompletada(completada);

        monitoreoService.actualizarEstadoRuta(id, estado);
    }

    private int calcularPedidosEntregados() {
        return (int) pedidosAsignados.stream()
                .filter(Pedido::isEntregado)
                .count();
    }

    @Override
    public String toString() {
        return "Ruta{" +
                "id='" + id + '\'' +
                ", camion='" + codigoCamion + '\'' +
                ", pedidos=" + pedidosAsignados.size() +
                ", nodos=" + secuenciaNodos.size() +
                ", distancia=" + distanciaTotal + "km" +
                ", consumo=" + String.format("%.2f", consumoCombustible) + "gal" +
                ", completada=" + completada +
                ", movimientoGenerado=" + movimientoGenerado +
                '}';
    }
}