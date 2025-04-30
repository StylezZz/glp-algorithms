package com.glp.glpDP1.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una ruta para un camión, con una secuencia de nodos y pedidos asignados
 */
@Getter @Setter
public class Ruta {
    private final String id;
    private final String codigoCamion;
    private List<Ubicacion> secuenciaNodos;
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

    public Ruta(String codigoCamion, Ubicacion origen) {
        this.id = UUID.randomUUID().toString();
        this.codigoCamion = codigoCamion;
        this.origen = origen;
        this.secuenciaNodos = new ArrayList<>();
        this.pedidosAsignados = new ArrayList<>();
        this.distanciaTotal = 0;
        this.consumoCombustible = 0;
        this.completada = false;
        this.cancelada = false;
        this.eventos = new ArrayList<>();
    }

    public List<Ubicacion> getSecuenciaNodos() {
        return new ArrayList<>(secuenciaNodos);
    }

    public void setSecuenciaNodos(List<Ubicacion> secuenciaNodos) {
        this.secuenciaNodos = new ArrayList<>(secuenciaNodos);
        calcularDistanciaTotal();
    }

    public List<Pedido> getPedidosAsignados() {
        return new ArrayList<>(pedidosAsignados);
    }

    public void setPedidosAsignados(List<Pedido> pedidosAsignados) {
        this.pedidosAsignados = new ArrayList<>(pedidosAsignados);
    }

    public List<EventoRuta> getEventos() {
        return new ArrayList<>(eventos);
    }

    /**
     * Añade un pedido a la ruta
     * @param pedido Pedido a añadir
     */
    public void agregarPedido(Pedido pedido) {
        if (!pedidosAsignados.contains(pedido)) {
            pedidosAsignados.add(pedido);
            pedido.setCamionAsignado(codigoCamion);

            // Añadir el nodo del pedido a la secuencia si no existe
            if (!secuenciaNodos.contains(pedido.getUbicacion())) {
                secuenciaNodos.add(pedido.getUbicacion());
                calcularDistanciaTotal();
            }
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

            // Verificar si hay que eliminar el nodo
            boolean hayOtroPedidoEnMismoNodo = false;
            Ubicacion ubicacionPedido = pedidoAEliminar.getUbicacion();

            for (Pedido p : pedidosAsignados) {
                if (p.getUbicacion().equals(ubicacionPedido)) {
                    hayOtroPedidoEnMismoNodo = true;
                    break;
                }
            }

            if (!hayOtroPedidoEnMismoNodo) {
                secuenciaNodos.remove(ubicacionPedido);
                calcularDistanciaTotal();
            }

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
     * considerando los bloqueos en el mapa
     * @param mapa Mapa con la información de bloqueos
     * @param momento Momento actual para verificar bloqueos
     */
    public void optimizarSecuenciaConBloqueos(Mapa mapa, LocalDateTime momento) {
        if (secuenciaNodos.size() <= 1) {
            return;
        }

        List<Ubicacion> nuevaSecuencia = new ArrayList<>();
        List<Ubicacion> pendientes = new ArrayList<>(secuenciaNodos);

        Ubicacion actual = origen;
        while (!pendientes.isEmpty()) {
            // Encontrar la mejor ruta al siguiente punto considerando bloqueos
            Ubicacion mejorSiguiente = null;
            List<Ubicacion> mejorRuta = null;

            for (Ubicacion destino : pendientes) {
                // Usar A* para encontrar la ruta óptima evitando bloqueos
                List<Ubicacion> ruta = mapa.encontrarRuta(actual, destino, momento);

                // Si no hay ruta posible por bloqueos, intentar con el siguiente destino
                if (ruta.isEmpty()) continue;

                // Calcular distancia real de la ruta (suma de tramos)
                int distanciaRuta = calcularDistanciaRuta(ruta);

                // Actualizar si es la mejor opción encontrada hasta ahora
                if (mejorRuta == null || distanciaRuta < calcularDistanciaRuta(mejorRuta)) {
                    mejorSiguiente = destino;
                    mejorRuta = ruta;
                }
            }

            // Si no se encontró ninguna ruta viable, salir
            if (mejorSiguiente == null) break;

            // Agregar el mejor destino a la secuencia
            nuevaSecuencia.add(mejorSiguiente);
            pendientes.remove(mejorSiguiente);
            actual = mejorSiguiente;
        }

        // Actualizar la secuencia de nodos
        this.secuenciaNodos = nuevaSecuencia;
        calcularDistanciaTotal();
    }

    // Método auxiliar para calcular la distancia total de una ruta
    private int calcularDistanciaRuta(List<Ubicacion> ruta) {
        int distancia = 0;
        for (int i = 0; i < ruta.size() - 1; i++) {
            distancia += ruta.get(i).distanciaA(ruta.get(i + 1));
        }
        return distancia;
    }

    /**
     * Optimiza la secuencia de nodos para minimizar la distancia total
     */
    public void optimizarSecuencia() {
        // Implementación simple de optimización (nearest neighbor)
        if (secuenciaNodos.size() <= 1) {
            return;
        }

        List<Ubicacion> nuevaSecuencia = new ArrayList<>();
        List<Ubicacion> pendientes = new ArrayList<>(secuenciaNodos);

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

            nuevaSecuencia.add(masProxima);
            pendientes.remove(masProxima);
            actual = masProxima;
        }

        this.secuenciaNodos = nuevaSecuencia;
        calcularDistanciaTotal();
    }

    public void optimizarConRecargas(Mapa mapa, Camion camion) {
        // Primero optimizar la secuencia base
        optimizarSecuencia();

        List<Ubicacion> nuevaSecuencia = new ArrayList<>();
        List<Almacen> almacenes = mapa.getAlmacenes();

        // Obtener almacén más cercano (para recarga)
        Almacen almacenCentral = mapa.obtenerAlmacenCentral();

        Ubicacion actual = origen;
        double combustibleActual = camion.getNivelCombustibleActual();

        for (Ubicacion siguiente : secuenciaNodos) {
            int distanciaAlSiguiente = actual.distanciaA(siguiente);

            // Verificar si tenemos suficiente combustible para llegar al siguiente punto y regresar al almacén
            Almacen almacenMasCercanoASiguiente = mapa.obtenerAlmacenMasCercano(siguiente);
            int distanciaAlAlmacenDesdeSiguiente = siguiente.distanciaA(almacenMasCercanoASiguiente.getUbicacion());

            double consumoHastaSiguiente = camion.calcularConsumoCombustible(distanciaAlSiguiente);
            double consumoDeRegresoAlAlmacen = camion.calcularConsumoCombustible(distanciaAlAlmacenDesdeSiguiente);
            double consumoTotal = consumoHastaSiguiente + consumoDeRegresoAlAlmacen;

            // Si no hay suficiente combustible, buscar recarga
            if (combustibleActual < consumoTotal) {
                // Buscar almacén más cercano a la ubicación actual
                Almacen almacenMasCercano = mapa.obtenerAlmacenMasCercano(actual);

                // Añadir desvío al almacén para recargar
                nuevaSecuencia.add(almacenMasCercano.getUbicacion());

                // Registrar el evento de recarga
                registrarEvento(
                        EventoRuta.TipoEvento.RECARGA_COMBUSTIBLE,
                        LocalDateTime.now() ,
                        almacenMasCercano.getUbicacion(),
                        "Recarga de combustible en " + almacenMasCercano.getId()
                );

                // Actualizar el combustible (tanque lleno)
                combustibleActual = camion.getCapacidadTanqueCombustible();

                // Ahora vamos desde el almacén al siguiente punto
                actual = almacenMasCercano.getUbicacion();
                distanciaAlSiguiente = actual.distanciaA(siguiente);
                consumoHastaSiguiente = camion.calcularConsumoCombustible(distanciaAlSiguiente);
            }

            // Añadir el punto a la secuencia
            nuevaSecuencia.add(siguiente);

            // Actualizar combustible y posición actual
            combustibleActual -= consumoHastaSiguiente;
            actual = siguiente;
        }

        // Actualizar la secuencia con las recargas
        this.secuenciaNodos = nuevaSecuencia;
        calcularDistanciaTotal();
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
                '}';
    }
}
