package com.glp.glpDP1.api.dto.websocket;

import com.glp.glpDP1.domain.enums.TipoIncidente;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ComandoFromFrontend {
    private String tipo; // "NEXT_INTERVAL", "GENERAR_AVERIA", "PAUSAR_SIMULACION", etc.
    private String codigoCamion;
    private TipoIncidente tipoIncidente;
    private LocalDateTime momentoSimulacion;
    private Map<String, Object> parametros;
}