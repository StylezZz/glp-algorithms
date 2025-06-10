package com.glp.glpDP1.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.glp.glpDP1.api.dto.websocket.ComandoFromFrontend;
import com.glp.glpDP1.api.dto.websocket.EstadoSimulacionResponse;
import com.glp.glpDP1.api.dto.websocket.WebSocketMessage;
import com.glp.glpDP1.services.SimulationStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationStateService simulationStateService;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SimulationWebSocketHandler(SimulationStateService simulationStateService) {
        this.simulationStateService = simulationStateService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        activeSessions.put(session.getId(), session);
        log.info("Cliente conectado: {}", session.getId());

        // Enviar mensaje de bienvenida
        WebSocketMessage bienvenida = new WebSocketMessage("CONNECTION_ESTABLISHED",
                Map.of("sessionId", session.getId(), "status", "connected"));
        enviarMensaje(session, bienvenida);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ComandoFromFrontend comando = objectMapper.readValue(
                    message.getPayload(), ComandoFromFrontend.class);

            log.debug("Comando recibido de {}: {}", session.getId(), comando.getTipo());

            procesarComando(session, comando);

        } catch (Exception e) {
            log.error("Error procesando mensaje de {}: {}", session.getId(), e.getMessage());
            enviarError(session, "Error procesando comando: " + e.getMessage());
        }
    }

    private void procesarComando(WebSocketSession session, ComandoFromFrontend comando) throws Exception {
        switch (comando.getTipo()) {
            case "NEXT_INTERVAL" -> {
                if (!simulationStateService.isSimulacionActiva()) {
                    enviarError(session, "La simulación no está activa");
                    return;
                }

                LocalDateTime momento = comando.getMomentoSimulacion() != null ?
                        comando.getMomentoSimulacion() : LocalDateTime.now();

                EstadoSimulacionResponse estado = simulationStateService.obtenerEstadoProximos15Min(momento);

                WebSocketMessage respuesta = new WebSocketMessage("SIMULATION_STATE", estado);
                enviarMensaje(session, respuesta);
            }

            case "GENERAR_AVERIA" -> {
                if (comando.getCodigoCamion() == null || comando.getTipoIncidente() == null) {
                    enviarError(session, "Faltan parámetros para generar avería");
                    return;
                }

                LocalDateTime momentoAveria = comando.getMomentoSimulacion() != null ?
                        comando.getMomentoSimulacion() : LocalDateTime.now();

                simulationStateService.generarAveria(
                        comando.getCodigoCamion(),
                        comando.getTipoIncidente(),
                        momentoAveria
                );

                // Notificar a todos los clientes sobre la avería
                WebSocketMessage notificacionAveria = new WebSocketMessage("AVERIA_GENERADA",
                        Map.of(
                                "camion", comando.getCodigoCamion(),
                                "tipo", comando.getTipoIncidente(),
                                "momento", momentoAveria,
                                "mensaje", "Avería generada y replanificación en proceso"
                        ));
                broadcast(notificacionAveria);
            }

            case "PAUSAR_SIMULACION" -> {
                simulationStateService.pausarSimulacion();
                WebSocketMessage pausada = new WebSocketMessage("SIMULATION_PAUSED",
                        Map.of("mensaje", "Simulación pausada"));
                broadcast(pausada);
            }

            case "REANUDAR_SIMULACION" -> {
                simulationStateService.reanudarSimulacion();
                WebSocketMessage reanudada = new WebSocketMessage("SIMULATION_RESUMED",
                        Map.of("mensaje", "Simulación reanudada"));
                broadcast(reanudada);
            }

            case "FINALIZAR_SIMULACION" -> {
                simulationStateService.finalizarSimulacion();
                WebSocketMessage finalizada = new WebSocketMessage("SIMULATION_ENDED",
                        Map.of("mensaje", "Simulación finalizada"));
                broadcast(finalizada);
            }

            default -> {
                log.warn("Comando no reconocido: {}", comando.getTipo());
                enviarError(session, "Comando no reconocido: " + comando.getTipo());
            }
        }
    }

    private void enviarMensaje(WebSocketSession session, WebSocketMessage mensaje) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(mensaje);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Error enviando mensaje a {}: {}", session.getId(), e.getMessage());
        }
    }

    private void enviarError(WebSocketSession session, String error) {
        WebSocketMessage errorMsg = new WebSocketMessage("ERROR",
                Map.of("mensaje", error, "timestamp", LocalDateTime.now()));
        enviarMensaje(session, errorMsg);
    }

    private void broadcast(WebSocketMessage mensaje) {
        activeSessions.values().forEach(session -> enviarMensaje(session, mensaje));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Error de transporte en sesión {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        activeSessions.remove(session.getId());
        log.info("Cliente desconectado: {} - {}", session.getId(), closeStatus);
    }
}