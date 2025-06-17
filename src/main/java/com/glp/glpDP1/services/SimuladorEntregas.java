package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Camion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Motor principal de la simulación:
 *   • Mantiene la flota en memoria
 *   • Avanza los “ticks” y actualiza posiciones
 *   • Expone la flota para que otros componentes (Publisher, REST, WS) la consulten
 */
@Service                              // ← Spring lo detecta como bean
@Getter                               // ← genera getFlota()
@Slf4j
public class SimuladorEntregas {

    /** Listado seguro para concurrencia */
    private final List<Camion> flota = new CopyOnWriteArrayList<>();

    /* ------------------------------------------------------------------ *
     *  Métodos públicos                                                  *
     * ------------------------------------------------------------------ */

    /** Vuelve a cargar la flota (p.e. al iniciar una nueva simulación) */
    public void inicializarFlota(List<Camion> camiones) {
        flota.clear();
        flota.addAll(camiones);
        log.info("Flota inicializada con {} camiones", flota.size());
    }

    /** Añade un camión individual si lo necesitas dinámicamente */
    public void agregarCamion(Camion camion) {
        flota.add(camion);
    }

    /** Detiene y vacía la simulación (opcional) */
    public void limpiar() {
        flota.clear();
        log.info("Flota limpiada");
    }

    /** Avanza un “tick” (ejemplo trivial) */
    public void avanzar() {
        flota.forEach(c -> {
            // TODO lógica de movimiento, consumo, etc.
            // e.g. c.getUbicacionActual().moverHacia(...)
        });
    }

    /** Devuelve una vista inmutable si necesitas exponerla fuera */
    public List<Camion> obtenerFlotaInmutable() {
        return Collections.unmodifiableList(flota);
    }
}
