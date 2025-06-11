package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.Ubicacion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MonitoreoService {
    private Map<String,EstadoRuta> estadosRuta = new ConcurrentHashMap<>();
    private Map<String,List<Bloqueo>> bloqueosPorZona = new ConcurrentHashMap<>();

    /**
     * Actualiza el estado de una ruta en tiempo real
     */
    public void actualizarEstadoRuta(String idRuta,EstadoRuta estado){
        estadosRuta.put(idRuta,estado);
    }

    public Map<String,EstadoRuta> obtenerEstadoRutas(){
        return new HashMap<>(estadosRuta);
    }

    public List<Bloqueo> obtenerBloqueosActivos(LocalDateTime momento){
        List<Bloqueo> bloqueosActivos = new ArrayList<>();
        for(List<Bloqueo> bloqueos: bloqueosPorZona.values()){
            for(Bloqueo bloqueo: bloqueos){
                if ((momento.isAfter(bloqueo.getHoraInicio()) || momento.isEqual(bloqueo.getHoraInicio())) &&
                        (momento.isBefore(bloqueo.getHoraFin()) || momento.isEqual(bloqueo.getHoraFin()))) {
                    bloqueosActivos.add(bloqueo);
                }
            }
        }

        return bloqueosActivos;
    }

    public void actualizarBloqueos(List<Bloqueo> bloqueos){
        Map<String,List<Bloqueo>> nuevosBloqueosPorZona = new HashMap<>();
        for(Bloqueo bloqueo: bloqueos){
            String zona = determinarZona(bloqueo.getNodosBloqueados());
            if(!nuevosBloqueosPorZona.containsKey(zona)){
                nuevosBloqueosPorZona.put(zona,new ArrayList<>());
            }
            nuevosBloqueosPorZona.get(zona).add(bloqueo);
        }
        this.bloqueosPorZona.clear();
        this.bloqueosPorZona.putAll(nuevosBloqueosPorZona);
    }

    private String determinarZona(List<Ubicacion> nodos){
        int sumX=0, sumY=0;
        for(Ubicacion nodo:nodos){
            sumX += nodo.getX();
            sumY += nodo.getY();
        }

        int centroideX = sumX / nodos.size();
        int centroideY = sumY / nodos.size();

        int zonaX = centroideX / 10; // Asumiendo que cada zona es de 10x10
        int zonaY = centroideY / 10;

        return zonaX + "-" + zonaY;
    }

    public void simularAvanceRutas(List<Ruta>rutas,double velocidadSimulacion){
        for(Ruta ruta:rutas){
            EstadoRuta estadoRuta = estadosRuta.get(ruta.getId());
            if(estadoRuta != null){
                Ubicacion ubicacionActual = estadoRuta.getPosicionActual();
                List<Ubicacion> secuenciaNodos = ruta.getSecuenciaNodos();
                int indiceNodoActual = secuenciaNodos.indexOf(ubicacionActual);
                if(indiceNodoActual < secuenciaNodos.size()-1){
                    Ubicacion siguienteNodo = secuenciaNodos.get(indiceNodoActual+1);
                    double distancia = ubicacionActual.distanciaA(siguienteNodo);
                    double tiempoEstimado = distancia / velocidadSimulacion;
                    estadoRuta.setDistanciaRecorrida(estadoRuta.getDistanciaRecorrida() + distancia);
                    estadoRuta.setConsumoCombustible(estadoRuta.getConsumoCombustible() + (distancia * 0.1)); // Ejemplo de consumo
                    estadoRuta.setPosicionActual(siguienteNodo);
                    estadoRuta.setUltimaActualizacion(LocalDateTime.now());
                }
            }
        }
    }

    @Getter @Setter
    public static class EstadoRuta{
        private String idRuta;
        private String codigoCamion;
        private List<Ubicacion> nodosRecorridos = new ArrayList<>();
        private Ubicacion posicionActual;
        private int pedidosEntregados;
        private int pedidosTotales;
        private double consumoCombustible;
        private double distanciaRecorrida;
        private LocalDateTime ultimaActualizacion;
        private boolean completada;
    }

    @Scheduled(fixedRate = 5000)
    public void verificarEstadoRutas(){
        LocalDateTime ahora = LocalDateTime.now();
        estadosRuta.entrySet().removeIf(entry ->
                entry.getValue().isCompletada() &&
                        entry.getValue().getUltimaActualizacion().plusHours(1).isBefore(ahora)
        );
    }



}
