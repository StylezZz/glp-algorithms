package com.glp.glpDP1.domain;

import com.glp.glpDP1.domain.enums.TipoAlmacen;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa un almacén de GLP, ya sea la planta principal o tanques intermedios
 */
@Getter @Setter
public class Almacen {
    private final String id;
    private Ubicacion ubicacion;
    private final TipoAlmacen tipo;
    private final double capacidadMaxima; // en m3
    private double nivelActual; // en m3
    private final LocalDateTime horaRecarga;

    public Almacen(String id, Ubicacion ubicacion, TipoAlmacen tipo, double capacidadMaxima) {
        this.id = id;
        this.ubicacion = ubicacion;
        this.tipo = tipo;
        this.capacidadMaxima = capacidadMaxima;
        this.nivelActual = capacidadMaxima;

        // Los tanques intermedios se recargan a las 00:00
        if (tipo == TipoAlmacen.PRINCIPAL) {
            this.horaRecarga = null; // La planta principal siempre está abastecida
        } else {
            // Para tanques intermedios, la próxima recarga es a las 00:00
            LocalDateTime ahora = LocalDateTime.now();
            this.horaRecarga = ahora.withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    /**
     * Extrae una cantidad de GLP del almacén
     * @param cantidad Cantidad a extraer en m3
     * @return true si se pudo extraer, false si no hay suficiente
     */
    public synchronized boolean extraer(double cantidad) {
        if (cantidad <= nivelActual) {
            nivelActual -= cantidad;
            return true;
        }
        return false;
    }

    /**
     * Recarga el almacén, solo para tanques intermedios
     */
    public synchronized void recargar() {
        if (tipo != TipoAlmacen.PRINCIPAL) {
            nivelActual = capacidadMaxima;
        }
    }

    /**
     * Verifica si es hora de recargar el almacén
     * @param momento Momento actual
     * @return true si debe recargarse
     */
    public boolean esHoraDeRecargar(LocalDateTime momento) {
        return tipo != TipoAlmacen.PRINCIPAL &&
                momento.getHour() == 0 &&
                momento.getMinute() == 0;
    }
}
