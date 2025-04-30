package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interfaz para el servicio de optimización mediante algoritmo ACO
 */
public interface ACOService {

    /**
     * Ejecuta el algoritmo ACO para optimizar rutas
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa con información de la ciudad
     * @param momento Momento actual para planificación
     * @param escenario Escenario de simulación
     * @param numHormigas Número de hormigas en la colonia
     * @param numIteraciones Número máximo de iteraciones
     * @param alfa Importancia de las feromonas
     * @param beta Importancia de la heurística
     * @param rho Tasa de evaporación de feromonas
     * @param q0 Parámetro de exploración vs explotación (0-1)
     * @return ID de la ejecución
     */
    String ejecutarACO(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            Integer numHormigas,
            Integer numIteraciones,
            Double alfa,
            Double beta,
            Double rho,
            Double q0);

    /**
     * Ejecuta el algoritmo ACO con parámetros optimizados
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa con información de la ciudad
     * @param momento Momento actual para planificación
     * @param escenario Escenario de simulación
     * @return ID de la ejecución
     */
    String ejecutarACOOptimizado(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario);

    /**
     * Obtiene el estado de una ejecución
     * @param id ID de la ejecución
     * @return Estado actual
     */
    Map<String, Object> obtenerEstado(String id);

    /**
     * Obtiene los resultados de una ejecución completada
     * @param id ID de la ejecución
     * @return Resultados de la optimización
     */
    Map<String, Object> obtenerResultados(String id);

    /**
     * Cancela una ejecución en curso
     * @param id ID de la ejecución
     * @return true si se canceló, false si no
     */
    boolean cancelarEjecucion(String id);

    /**
     * Ejecuta múltiples instancias del algoritmo ACO y devuelve la mejor solución
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa con información de la ciudad
     * @param momento Momento actual para planificación
     * @param escenario Escenario de simulación
     * @param numEjecuciones Número de ejecuciones independientes
     * @return Resultado de la mejor ejecución
     */
    Map<String, Object> ejecutarMultipleACO(
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momento,
            EscenarioSimulacion escenario,
            int numEjecuciones);
}