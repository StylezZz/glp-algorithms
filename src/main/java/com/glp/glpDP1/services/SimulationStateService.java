package com.glp.glpDP1.services;

import com.glp.glpDP1.api.dto.websocket.EstadoSimulacionResponse;
import com.glp.glpDP1.domain.enums.TipoIncidente;

import java.time.LocalDateTime;

public interface SimulationStateService {
    void inicializarSimulacion(String algoritmoId, LocalDateTime fechaInicial);
    EstadoSimulacionResponse obtenerEstadoProximos15Min(LocalDateTime momentoActual);
    void generarAveria(String codigoCamion, TipoIncidente tipoIncidente, LocalDateTime momento);
    void pausarSimulacion();
    void reanudarSimulacion();
    boolean isSimulacionActiva();
    void finalizarSimulacion();
}