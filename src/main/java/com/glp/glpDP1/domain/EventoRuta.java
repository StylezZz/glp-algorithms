package com.glp.glpDP1.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EventoRuta {
    public enum TipoEvento {
        INICIO,
        ENTREGA,
        RECARGA_COMBUSTIBLE,
        RECARGA_GLP,
        FIN,
        MANTENIMIENTO,
        AVERIA_TI1,         // Nueva: Avería tipo 1 (ej: llanta baja)
        AVERIA_TI2,         // Nueva: Avería tipo 2 (ej: motor ahogado)
        AVERIA_TI3,         // Nueva: Avería tipo 3 (ej: choque)
        FIN_POR_AVERIA      // Nueva: Regreso al almacén por avería
    }

    private final TipoEvento tipo;
    private final LocalDateTime momento;
    private final Ubicacion ubicacion;
    private final String descripcion;

    public EventoRuta(TipoEvento tipo, LocalDateTime momento, Ubicacion ubicacion, String descripcion) {
        this.tipo = tipo;
        this.momento = momento;
        this.ubicacion = ubicacion;
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "[" + momento + "] " + tipo + " en " + ubicacion + ": " + descripcion;
    }
}