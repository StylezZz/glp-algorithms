package com.glp.glpDP1.api.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String tipo;
    private LocalDateTime timestamp;
    private Object datos;
    private String sessionId;

    public WebSocketMessage(String tipo, Object datos) {
        this.tipo = tipo;
        this.datos = datos;
        this.timestamp = LocalDateTime.now();
    }
}