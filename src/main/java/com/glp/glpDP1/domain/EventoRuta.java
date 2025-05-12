package com.glp.glpDP1.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EventoRuta {
    public enum TipoEvento {
        INICIO,
        ENTREGA,
        RECARGA_GLP,
        RECARGA_COMBUSTIBLE,
        AVERIA,
        TRASVASE,  // Nuevo tipo de evento
        BLOQUEO,
        FIN
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