package com.glp.glpDP1.domain.enums;

import lombok.Getter;

@Getter
public enum Turno {
    T1(0, 8),   // 00:00 - 08:00
    T2(8, 16),  // 08:00 - 16:00
    T3(16, 24); // 16:00 - 00:00

    private final int horaInicio;
    private final int horaFin;

    Turno(int horaInicio, int horaFin) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    /**
     * Obtiene el turno correspondiente a una hora del día
     * @param hora Hora del día (0-23)
     * @return Turno correspondiente
     */
    public static Turno obtenerTurnoPorHora(int hora) {
        if (hora >= 0 && hora < 8) {
            return T1;
        } else if (hora >= 8 && hora < 16) {
            return T2;
        } else {
            return T3;
        }
    }
}
