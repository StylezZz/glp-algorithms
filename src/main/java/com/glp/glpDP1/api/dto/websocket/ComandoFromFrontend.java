// src/main/java/com/glp/glpDP1/api/dto/websocket/ComandoFromFrontend.java
package com.glp.glpDP1.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)   // ← ignora cualquier campo que no mapees
public class ComandoFromFrontend {

    private String tipo;              // INICIAR_SIMULACION, GENERAR_AVERIA …
    private String modo;              // daily | weekly | collapse   ← NUEVO
    private String codigoCamion;
    private String tipoIncidente;
    private LocalDateTime momentoSimulacion;   // opcional
    private Map<String, Object> parametros;    // por si necesitas extras
}
