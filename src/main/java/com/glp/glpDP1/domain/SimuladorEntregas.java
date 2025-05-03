package com.glp.glpDP1.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimuladorEntregas {
    private final double velocidadPromedio = 50.0; // km/h
    private final int tiempoDescargaMinutos = 15; // minutos por entrega

    /**
     * Simula la ejecución de una lista de rutas
     * @param rutas Lista de rutas generadas por el algoritmo
     * @param momentoInicio Momento de inicio de la simulación
     * @return Las rutas con los tiempos de entrega actualizados
     */
    public List<Ruta> simularEntregas(List<Ruta> rutas, LocalDateTime momentoInicio) {
        for (Ruta ruta : rutas) {
            simularRuta(ruta, momentoInicio);
        }
        return rutas;
    }

    /**
     * Simula la ejecución de una ruta específica
     * @param ruta Ruta a simular
     * @param momentoInicio Momento de inicio
     */
    private void simularRuta(Ruta ruta, LocalDateTime momentoInicio) {
        // Establecer hora de inicio
        ruta.setHoraInicio(momentoInicio);

        // Obtener ubicaciones
        Ubicacion ubicacionActual = ruta.getOrigen();
        List<Ubicacion> secuencia = ruta.getSecuenciaNodos();

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

        // Momento actual de la simulación
        LocalDateTime momentoActual = momentoInicio;

        // Simular el recorrido
        for (Ubicacion nodo : secuencia) {
            // Calcular distancia y tiempo hasta el siguiente nodo
            int distancia = ubicacionActual.distanciaA(nodo);
            double horasViaje = distancia / velocidadPromedio;
            long minutosViaje = Math.round(horasViaje * 60);

            // Avanzar el tiempo
            momentoActual = momentoActual.plusMinutes(minutosViaje);

            // Verificar si hay pedidos en este nodo
            if (pedidosPorUbicacion.containsKey(nodo)) {
                List<Pedido> pedidosEnNodo = pedidosPorUbicacion.get(nodo);

                for (Pedido pedido : pedidosEnNodo) {
                    // Programar hora de entrega (igual al momento de llegada)
                    pedido.setHoraEntregaProgramada(momentoActual);

                    // Establecer hora de entrega real (incluye el tiempo de descarga)
                    pedido.setHoraEntregaReal(momentoActual);

                    // Marcar como entregado
                    pedido.setEntregado(true);

                    // Registrar evento de entrega
                    ruta.registrarEvento(
                            EventoRuta.TipoEvento.ENTREGA,
                            momentoActual,
                            nodo,
                            "Entrega completada para cliente " + pedido.getIdCliente()
                    );

                    // Agregar tiempo de descarga para el siguiente pedido
                    momentoActual = momentoActual.plusMinutes(tiempoDescargaMinutos);
                }
            }

            // Actualizar ubicación actual
            ubicacionActual = nodo;
        }

        // Calcular tiempo de regreso al destino (almacén)
        int distanciaRegreso = ubicacionActual.distanciaA(ruta.getDestino());
        double horasRegreso = distanciaRegreso / velocidadPromedio;
        long minutosRegreso = Math.round(horasRegreso * 60);

        // Actualizar hora final
        momentoActual = momentoActual.plusMinutes(minutosRegreso);
        ruta.setHoraFinEstimada(momentoActual);
        ruta.setHoraFinReal(momentoActual);

        // Marcar ruta como completada
        ruta.setCompletada(true);

        // Registrar evento de finalización
        ruta.registrarEvento(
                EventoRuta.TipoEvento.FIN,
                momentoActual,
                ruta.getDestino(),
                "Ruta completada con éxito"
        );
    }
}
