package com.glp.glpDP1.repository;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;

import java.util.List;

/**
 * Repositorio para los datos de la aplicación (en memoria)
 */
public interface DataRepository {

    /**
     * Guarda el mapa en el repositorio
     */
    void guardarMapa(Mapa mapa);

    /**
     * Obtiene el mapa actual
     */
    Mapa obtenerMapa();

    /**
     * Guarda los camiones en el repositorio
     */
    void guardarCamiones(List<Camion> camiones);

    /**
     * Obtiene la lista de camiones
     */
    List<Camion> obtenerCamiones();

    /**
     * Busca un camión por su código
     */
    Camion buscarCamion(String codigo);

    /**
     * Guarda los pedidos en el repositorio
     */
    void guardarPedidos(List<Pedido> pedidos);

    /**
     * Obtiene la lista de pedidos
     */
    List<Pedido> obtenerPedidos();

    /**
     * Guarda los bloqueos en el repositorio
     */
    void guardarBloqueos(List<Bloqueo> bloqueos);

    /**
     * Obtiene la lista de bloqueos
     */
    List<Bloqueo> obtenerBloqueos();
}