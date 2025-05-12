package com.glp.glpDP1.domain;

import com.glp.glpDP1.services.impl.MonitoreoService;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Representa una ruta para un camión, con una secuencia de nodos y pedidos asignados
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
    private List<DetalleTrasvase> trasvases;
    private int pedidosRetrasados;
    private double porcentajeRetrasos;

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
        this.requiereReabastecimiento = false;  // NUEVO: Inicializar a false
        this.trasvases = new ArrayList<>();
        this.pedidosRetrasados = 0;
        this.porcentajeRetrasos = 0.0;

    }

    /**
     * Agregar un detalle de trasvase a la ruta
     *
     */
    public void agregarTrasvase(DetalleTrasvase trasvase){
        if(this.trasvases == null){
            this.trasvases = new ArrayList<>();
        }
        this.trasvases.add(trasvase);
    }

    /**
     * Obtiene la lista de trasvases
     *
     */
    public List<DetalleTrasvase> getTravases(){
        if(this.trasvases == null){
            this.trasvases = new ArrayList<>();
        }
        return this.trasvases;
    }

    /**
     * Genera un informe detallado de los trasvases realizados en esta ruta
     */
    public String generarInformeTrasvases(){
        if (getTrasvases().isEmpty()) {
            return "No se realizaron trasvases en esta ruta.";
        }
        
        StringBuilder informe = new StringBuilder();
        informe.append("INFORME DE TRASVASES - Ruta ").append(id)
               .append(" (Camión ").append(codigoCamion).append(")\n\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (int i = 0; i < trasvases.size(); i++) {
            DetalleTrasvase t = trasvases.get(i);
            informe.append("Trasvase #").append(i+1).append("\n");
            informe.append("- Origen: Camión ").append(t.getCamionOrigen()).append("\n");
            informe.append("- Destino: Camión ").append(t.getCamionDestino()).append("\n");
            informe.append("- Ubicación: ").append(t.getUbicacion()).append("\n");
            informe.append("- Inicio: ").append(t.getMomentoInicio().format(formatter)).append("\n");
            informe.append("- Fin: ").append(t.getMomentoFin().format(formatter)).append("\n");
            informe.append("- Duración: ").append(t.getDuracionMinutos()).append(" minutos\n");
            informe.append("- Volumen GLP: ").append(String.format("%.2f m³", t.getCantidadGLP())).append("\n");
            informe.append("- Pedidos transferidos: ").append(t.getPedidosTransferidos().size()).append("\n");
            
            // Detalles de cada pedido
            informe.append("  Detalle de pedidos:\n");
            for (Pedido p : t.getPedidosTransferidos()) {
                informe.append("  * Cliente: ").append(p.getIdCliente())
                       .append(", Vol: ").append(String.format("%.2f m³", p.getCantidadGLP()))
                       .append(", Plazo: ").append(p.getHoraLimiteEntrega().format(formatter))
                       .append("\n");
            }
            
            informe.append("\n");
        }
        
        return informe.toString();
    }

    /**
     * Establece el número y porcentaje de pedidos retrasados
     */
    public void setEstadisticasRetrasos(int pedidosRetrasados, double porcentajeRetrasos) {
        this.pedidosRetrasados = pedidosRetrasados;
        this.porcentajeRetrasos = porcentajeRetrasos;
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

            // MODIFICADO: verificar si hay que eliminar de PARADAS
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
    
        // IMPORTANTE: Actualizar el valor del consumo en el objeto
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
    public void  optimizarSecuenciaConBloqueos(Mapa mapa,
                                               LocalDateTime momento,
                                               Camion camion) {
        if (secuenciaParadas.size() <= 1) return;

        List<Ubicacion> nuevaSecuenciaParadas = new ArrayList<>();
        List<Ubicacion> todosLosNodos = new ArrayList<>();
        todosLosNodos.add(origen);

        Ubicacion actual = origen;
        LocalDateTime tiempoActual = momento;
        double combustibleActual = camion.getNivelCombustibleActual();

        for (Ubicacion siguiente : secuenciaParadas) {
            // Calcular ruta A*
            List<Ubicacion> rutaDetallada = mapa.encontrarRutaConTiempo(
                    actual, siguiente, tiempoActual, camion.getVelocidadPromedio());

            if (rutaDetallada.isEmpty()) {
                // No hay ruta posible
                this.factibilidadEncontrada = false;
                return;
            }

            int distanciaTotal = calcularDistanciaRuta(rutaDetallada);
            double consumoNecesario = camion.calcularConsumoCombustible(distanciaTotal);

            // VERIFICAR COMBUSTIBLE ANTES DE COMPROMETERSE
            if (combustibleActual < consumoNecesario) {
                // Encontrar almacén más cercano
                Almacen almacenCercano = mapa.obtenerAlmacenMasCercano(actual);

                // Ruta a almacén + almacén a siguiente destino
                List<Ubicacion> rutaAlAlmacen = mapa.encontrarRutaConTiempo(
                        actual, almacenCercano.getUbicacion(), tiempoActual, camion.getVelocidadPromedio());

                if (rutaAlAlmacen.isEmpty()) {
                    this.factibilidadEncontrada = false;
                    return;
                }

                // Añadir parada en almacén
                nuevaSecuenciaParadas.add(almacenCercano.getUbicacion());
                todosLosNodos.addAll(rutaAlAlmacen);

                // Actualizar posición y combustible
                actual = almacenCercano.getUbicacion();
                combustibleActual = camion.getCapacidadTanqueCombustible();

                // Recalcular ruta desde almacén
                rutaDetallada = mapa.encontrarRutaConTiempo(
                        actual, siguiente, tiempoActual, camion.getVelocidadPromedio());
            }

            // Añadir destino original a la ruta
            nuevaSecuenciaParadas.add(siguiente);
            todosLosNodos.addAll(rutaDetallada);

            // Actualizar estado para siguiente iteración
            combustibleActual -= consumoNecesario;
            actual = siguiente;
            tiempoActual = calcularTiempoLlegada(rutaDetallada, tiempoActual, camion);
        }

        this.secuenciaParadas = nuevaSecuenciaParadas;
        this.secuenciaNodos = todosLosNodos;
        calcularDistanciaTotal();
    }

    /**
     * Calcula el tiempo de llegada siguiendo una ruta específica
     */
    private LocalDateTime calcularTiempoLlegada(List<Ubicacion> ruta,
                                                LocalDateTime tiempoInicio,
                                                Camion camion) {
        int distanciaTotal = calcularDistanciaRuta(ruta);
        double horasViaje = distanciaTotal / camion.getVelocidadPromedio();
        long minutosViaje = Math.round(horasViaje * 60);

        return tiempoInicio.plusMinutes(minutosViaje);
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

    /**
     * Optimiza la secuencia de nodos incluyendo paradas en el almacén central
     * para rutas que requieren más GLP del que un camión puede llevar en un viaje
     * @param camion Camión asignado a la ruta
     * @param mapa Mapa con información de la ciudad y almacenes
     */
    public void optimizarSecuenciaConReabastecimientoEnCentral(Camion camion, Mapa mapa) {
        if (secuenciaNodos.isEmpty() || secuenciaNodos.size() <= 1) {
            return;
        }

        // Obtener almacén central
        Almacen almacenCentral = mapa.obtenerAlmacenCentral();
        Ubicacion ubicacionAlmacenCentral = almacenCentral.getUbicacion();

        // Primero hacemos una optimización básica para ordenar los nodos
        optimizarSecuencia();

        // Lista para la nueva secuencia optimizada
        List<Ubicacion> nuevaSecuencia = new ArrayList<>();

        // Calcular el GLP total necesario para esta ruta
        double glpTotal = pedidosAsignados.stream()
                .mapToDouble(Pedido::getCantidadGLP)
                .sum();

        // Si no necesitamos más de lo que el camión puede llevar, no es necesario modificar
        if (glpTotal <= camion.getCapacidadTanqueGLP()) {
            return;
        }

        // Crear un mapa para asociar cada ubicación con su pedido y cantidad de GLP
        Map<Ubicacion, List<Double>> glpPorUbicacion = new HashMap<>();

        for (Pedido pedido : pedidosAsignados) {
            Ubicacion ubicacion = pedido.getUbicacion();
            if (!glpPorUbicacion.containsKey(ubicacion)) {
                glpPorUbicacion.put(ubicacion, new ArrayList<>());
            }
            glpPorUbicacion.get(ubicacion).add(pedido.getCantidadGLP());
        }

        // Variable para rastrear la capacidad restante
        double capacidadRestante = camion.getCapacidadTanqueGLP();
        Ubicacion ubicacionActual = origen;

        // Procesar cada ubicación en la secuencia optimizada
        for (Ubicacion ubicacion : secuenciaNodos) {
            // Calcular GLP total necesario para esta ubicación
            double glpNecesario = glpPorUbicacion.get(ubicacion).stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            // Si no hay suficiente capacidad, necesitamos volver al almacén central
            if (glpNecesario > capacidadRestante) {
                // Añadir el almacén central a la ruta
                nuevaSecuencia.add(ubicacionAlmacenCentral);

                // Registrar el evento de recarga de GLP
                registrarEvento(
                        EventoRuta.TipoEvento.RECARGA_GLP,
                        LocalDateTime.now(),
                        ubicacionAlmacenCentral,
                        "Recarga de GLP en almacén central"
                );

                // Restaurar capacidad completa
                capacidadRestante = camion.getCapacidadTanqueGLP();
                ubicacionActual = ubicacionAlmacenCentral;
            }

            // Añadir la ubicación actual a la secuencia
            nuevaSecuencia.add(ubicacion);

            // Reducir la capacidad disponible
            capacidadRestante -= glpNecesario;
            ubicacionActual = ubicacion;
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
     * Calcula los tiempos estimados de llegada a cada nodo en la ruta
     * @param momentoInicio Momento de inicio de la ruta
     * @param velocidadKmH Velocidad del camión en km/h
     * @return Mapa con los tiempos estimados de llegada a cada ubicación
     */
    public Map<Ubicacion, LocalDateTime> calcularTiemposLlegada(LocalDateTime momentoInicio, double velocidadKmH) {
        Map<Ubicacion, LocalDateTime> tiemposLlegada = new HashMap<>();
        LocalDateTime tiempoActual = momentoInicio;
        Ubicacion actual = origen;

        for (Ubicacion siguiente : secuenciaNodos) {
            int distancia = actual.distanciaA(siguiente);
            // Convertir distancia a segundos según velocidad (50 km/h = 72 segundos por km)
            long segundosViaje = (long) ((double) distancia / velocidadKmH * 3600);
            tiempoActual = tiempoActual.plusSeconds(segundosViaje);
            tiemposLlegada.put(siguiente, tiempoActual);
            actual = siguiente;
        }

        return tiemposLlegada;
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

        // NUEVO: también actualizar la secuencia completa de nodos
        this.secuenciaNodos = new ArrayList<>(secuenciaParadas);
        this.secuenciaNodos.add(0, origen);

        calcularDistanciaTotal();
    }

    public void optimizarConRecargas(Mapa mapa, Camion camion) {
        // Primero optimizar la secuencia base
        optimizarSecuencia();

        List<Ubicacion> nuevaSecuenciaParadas = new ArrayList<>();
        List<Almacen> almacenes = mapa.getAlmacenes();

        // MODIFICADO: Obtener almacén central (para priorizar)
        Almacen almacenCentral = mapa.obtenerAlmacenCentral();

        Ubicacion actual = origen;
        double combustibleActual = camion.getNivelCombustibleActual();

        double glpTotal = pedidosAsignados.stream().mapToDouble(Pedido::getCantidadGLP).sum();

        boolean necesitaRecargaGLP = glpTotal > camion.getCapacidadTanqueGLP();

        // MODIFICADO: trabajar con secuenciaParadas
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

            // Verificar si tenemos suficiente combustible para llegar al siguiente punto y regresar al almacén
            Almacen almacenMasCercanoASiguiente = mapa.obtenerAlmacenMasCercano(siguiente);
            int distanciaAlAlmacenDesdeSiguiente = siguiente.distanciaA(almacenMasCercanoASiguiente.getUbicacion());

            double consumoHastaSiguiente = camion.calcularConsumoCombustible(distanciaAlSiguiente);
            double consumoDeRegresoAlAlmacen = camion.calcularConsumoCombustible(distanciaAlAlmacenDesdeSiguiente);
            double consumoTotal = consumoHastaSiguiente + consumoDeRegresoAlAlmacen;

            // Si no hay suficiente combustible, buscar recarga
            if (combustibleActual < consumoTotal) {
                // MODIFICADO: Decidir qué almacén usar para recargar
                Almacen almacenParaRecargar;

                // Si la ruta tiene alta demanda de GLP o contiene pedidos urgentes, priorizar almacén central
                boolean hayPedidosUrgentes = pedidosAsignados.stream()
                        .anyMatch(p -> p.getTiempoLimiteEntrega().toHours() < 8);

                if (necesitaRecargaGLP || hayPedidosUrgentes) {
                    // Priorizar el almacén central para rutas importantes
                    almacenParaRecargar = almacenCentral;
                } else {
                    // Para rutas estándar, usar el almacén más cercano
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
