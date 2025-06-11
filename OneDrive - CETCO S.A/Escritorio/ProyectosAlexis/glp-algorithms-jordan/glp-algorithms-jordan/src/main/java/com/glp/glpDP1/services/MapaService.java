package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Mapa;

import java.util.List;

public interface MapaService {

    /**
     * Obtiene el mapa actual del sistema
     * @return Mapa con la configuración actual
     */
    Mapa obtenerMapa();

    /**
     * Actualiza los bloqueos en el mapa
     * @param bloqueos Lista de bloqueos a aplicar
     * @return Mapa actualizado
     */
    Mapa actualizarBloqueos(List<Bloqueo> bloqueos);

    /**
     * Añade un bloqueo al mapa
     * @param bloqueo Bloqueo a añadir
     * @return Mapa actualizado
     */
    Mapa agregarBloqueo(Bloqueo bloqueo);
}