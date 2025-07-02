package com.glp.glpDP1.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa un bloqueo temporal en la ciudad
 */
@Getter
@Setter
public class Bloqueo {
    private final String id;
    private final LocalDateTime horaInicio;
    private final LocalDateTime horaFin;
    private final List<Ubicacion> nodosBloqueados;

    public Bloqueo(LocalDateTime horaInicio, LocalDateTime horaFin, List<Ubicacion> nodos) {
        this.id = UUID.randomUUID().toString();
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.nodosBloqueados = new ArrayList<>(nodos);
    }

    public List<Ubicacion> getNodosBloqueados() {
        return new ArrayList<>(nodosBloqueados);
    }

    /**
     * Verifica si un nodo está bloqueado en un momento dado
     * @param nodo Nodo a verificar
     * @param momento Momento para la verificación
     * @return true si el nodo está bloqueado en ese momento
     */
    public boolean estaBloqueado(Ubicacion nodo, LocalDateTime momento) {
        return (momento.isAfter(horaInicio) || momento.isEqual(horaInicio)) &&
                (momento.isBefore(horaFin) || momento.isEqual(horaFin)) &&
                nodosBloqueados.contains(nodo);
    }

    /**
     * Verifica si un tramo entre dos nodos está bloqueado
     * @param origen Nodo de origen
     * @param destino Nodo de destino
     * @param momento Momento para la verificación
     * @return true si el tramo está bloqueado
     */
    public boolean tramoBloqueado(Ubicacion origen, Ubicacion destino, LocalDateTime momento) {
        // Si ambos nodos son adyacentes y uno está bloqueado, el tramo está bloqueado
        if (origen.distanciaA(destino) == 1) {
            return estaBloqueado(origen, momento) || estaBloqueado(destino, momento);
        }
        return false;
    }

    private Bloqueo mapearBloqueoDesdeSP(Object[] row) {
        LocalDateTime horaInicio = ((java.sql.Timestamp) row[0]).toLocalDateTime();
        LocalDateTime horaFin = ((java.sql.Timestamp) row[1]).toLocalDateTime();
        Integer latitud = ((Number) row[2]).intValue();
        Integer longitud = ((Number) row[3]).intValue();

        // Crea la lista de nodos bloqueados con un solo nodo por fila
        List<Ubicacion> nodos = new ArrayList<>();
        nodos.add(new Ubicacion(latitud, longitud));

        return new Bloqueo(horaInicio, horaFin, nodos);
    }
}