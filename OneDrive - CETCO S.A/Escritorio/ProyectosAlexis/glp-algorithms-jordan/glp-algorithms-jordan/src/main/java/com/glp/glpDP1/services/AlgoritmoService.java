package com.glp.glpDP1.services;

import com.glp.glpDP1.api.dto.request.AlgoritmoRequest;
import com.glp.glpDP1.api.dto.request.AlgoritmoSimpleRequest;
import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.api.dto.response.AlgoritmoStatusResponse;
import org.springframework.stereotype.Service;

@Service
public interface AlgoritmoService {

    /**
     * Inicia la ejecución de un algoritmo de optimización
     * @param request Parámetros para iniciar el algoritmo
     * @return Identificador único de la ejecución
     */
    String iniciarAlgoritmo(AlgoritmoRequest request);

    /**
     * Inicia la ejecución de un algoritmo de optimización usando datos ya cargados
     * @param request Parámetros simplificados para iniciar el algoritmo
     * @return Identificador único de la ejecución
     */
    String iniciarAlgoritmo(AlgoritmoSimpleRequest request);

    /**
     * Consulta el estado actual de la ejecución de un algoritmo
     * @param id Identificador de la ejecución
     * @return Estado actual del algoritmo
     */
    AlgoritmoStatusResponse consultarEstado(String id);

    /**
     * Obtiene los resultados de una ejecución completada
     * @param id Identificador de la ejecución
     * @return Resultados de la optimización
     */
    AlgoritmoResultResponse obtenerResultados(String id);

    /**
     * Cancela la ejecución en curso de un algoritmo
     * @param id Identificador de la ejecución
     * @return true si se pudo cancelar, false si no
     */
    boolean cancelarEjecucion(String id);
}