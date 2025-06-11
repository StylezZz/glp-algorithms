package com.glp.glpDP1.domain;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Ubicacion {
    private int x;
    private int y;

    public Ubicacion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Calcula la distancia Manhattan a otra ubicación
     * @param otra Ubicación destino
     * @return Distancia en kilómetros
     */
    public int distanciaA(Ubicacion otra) {
        return Math.abs(this.x - otra.x) + Math.abs(this.y - otra.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ubicacion ubicacion = (Ubicacion) o;
        return x == ubicacion.x && y == ubicacion.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}