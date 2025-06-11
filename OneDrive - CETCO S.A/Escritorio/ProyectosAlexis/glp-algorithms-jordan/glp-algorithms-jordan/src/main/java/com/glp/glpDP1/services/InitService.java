package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Almacen;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;

import java.io.InputStream;
import java.util.List;

/**
 * Servicio para inicializar los datos de la aplicación
 */
public interface InitService {

    /**
     * Inicializa los datos básicos (camiones, almacenes, mapa)
     */
    void inicializarDatos();

    /**
     * Carga los datos de camiones desde un archivo
     * @param inputStream Stream con el contenido del archivo
     * @return Lista de camiones cargados
     */
    List<Camion> cargarCamiones(InputStream inputStream);

    /**
     * Carga el plan de mantenimiento desde un archivo
     * @param inputStream Stream con el contenido del archivo
     * @return Número de registros de mantenimiento cargados
     */
    int cargarPlanMantenimiento(InputStream inputStream);

    /**
     * Obtiene el mapa actual
     * @return Mapa actual
     */
    Mapa obtenerMapa();

    /**
     * Obtiene la lista de camiones disponibles
     * @return Lista de camiones
     */
    List<Camion> obtenerCamiones();

    /**
     * Obtiene la lista de almacenes
     * @return Lista de almacenes
     */
    List<Almacen> obtenerAlmacenes();
}