package com.glp.glpDP1.algorithm;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa la asignación de un pedido a un camión
 */
@Getter @Setter
public class Asignacion {
    private final int indiceCamion;
    private final double valor;
    private final boolean parcial;
    private final double cantidadGLP;

    public Asignacion(int indiceCamion, double valor) {
        this.indiceCamion = indiceCamion;
        this.valor = valor;
        this.parcial = false;
        this.cantidadGLP = 0.0;
    }

    public Asignacion(int indiceCamion, double valor, boolean parcial, double cantidadGLP) {
        this.indiceCamion = indiceCamion;
        this.valor = valor;
        this.parcial = parcial;
        this.cantidadGLP = cantidadGLP;
    }

    public boolean esValida() {
        return indiceCamion >= 0;
    }

    public boolean esParcial() {
        return parcial;
    }

    public static Asignacion asignacionNula() {
        return new Asignacion(-1, 0.0);
    }

    public static Asignacion asignacionParcial(int indiceCamion, double valor, double cantidadGLP) {
        return new Asignacion(indiceCamion, valor, true, cantidadGLP);
    }
}