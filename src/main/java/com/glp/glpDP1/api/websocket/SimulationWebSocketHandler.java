package com.glp.glpDP1.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.glp.glpDP1.api.dto.websocket.ComandoFromFrontend;
import com.glp.glpDP1.api.dto.websocket.EstadoSimulacionResponse;
import com.glp.glpDP1.api.dto.websocket.WebSocketMessage;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.services.SimulationStateService;
import com.glp.glpDP1.services.SimuladorEntregas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WS “puro” (sin STOMP):
 *   • Atiende comandos del frontend (iniciar, pausar, averías…)
 *   • Emite, cada segundo, la posición de toda la flota.
 */
@Component
@Slf4j
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationStateService simulationStateService;
    private final SimuladorEntregas      simulador;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public SimulationWebSocketHandler(SimulationStateService simulationStateService,
                                      SimuladorEntregas simulador) {
        this.simulationStateService = simulationStateService;
        this.simulador             = simulador;
    }

    /* ══════════════════  CONEXIÓN / DESCONEXIÓN  ══════════════════ */

    @Override
    public void afterConnectionEstablished(WebSocketSession s) {
        activeSessions.put(s.getId(), s);
        log.info("Cliente conectado: {}", s.getId());
        enviarMensaje(s, new WebSocketMessage("CONNECTION_ESTABLISHED",
                Map.of("sessionId", s.getId(), "status", "connected")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus st) {
        activeSessions.remove(s.getId());
        log.info("Cliente desconectado: {} - {}", s.getId(), st);
    }

    @Override
    public void handleTransportError(WebSocketSession s, Throwable ex) {
        log.error("Error de transporte en {}: {}", s.getId(), ex.getMessage());
    }

    /* ═════════════════════  MENSAJES ENTRANTES  ═════════════════════ */

    @Override
    protected void handleTextMessage(WebSocketSession s, TextMessage raw) {
        try {
            ComandoFromFrontend cmd = mapper.readValue(raw.getPayload(), ComandoFromFrontend.class);
            log.debug("Comando {} de {}", cmd.getTipo(), s.getId());
            procesarComando(s, cmd);
        } catch (Exception ex) {
            log.error("Falló parseo/comando: {}", ex.getMessage());
            enviarError(s, "Error procesando comando: " + ex.getMessage());
        }
    }

    private void procesarComando(WebSocketSession s, ComandoFromFrontend cmd) {
        switch (cmd.getTipo()) {

            /* ========= 1. Arranque ========= */
            case "INICIAR_SIMULACION" -> {
                simulationStateService.inicializarSimulacion(
                        cmd.getModo() == null ? "daily" : cmd.getModo(),
                        LocalDateTime.now());

                broadcast(new WebSocketMessage("SIMULATION_STARTED",
                        Map.of("modo", cmd.getModo(), "mensaje", "Simulación iniciada")));
            }

            /* ========= 2. Siguiente intervalo ========= */
            case "NEXT_INTERVAL" -> {
                if (!simulationStateService.isSimulacionActiva()) {
                    enviarError(s, "La simulación no está activa");
                    return;
                }
                LocalDateTime t = cmd.getMomentoSimulacion() == null
                        ? LocalDateTime.now()
                        : cmd.getMomentoSimulacion();

                EstadoSimulacionResponse e = simulationStateService.obtenerEstadoProximos15Min(t);
                enviarMensaje(s, new WebSocketMessage("SIMULATION_STATE", e));
            }

            /* ========= 3. Avería manual ========= */
            case "GENERAR_AVERIA" -> {
                if (cmd.getCodigoCamion() == null || cmd.getTipoIncidente() == null) {
                    enviarError(s, "Faltan parámetros para generar avería");
                    return;
                }
                LocalDateTime t = cmd.getMomentoSimulacion() == null
                        ? LocalDateTime.now()
                        : cmd.getMomentoSimulacion();

                TipoIncidente tipo;
                try {
                    tipo = TipoIncidente.valueOf(cmd.getTipoIncidente());
                } catch (IllegalArgumentException ex) {
                    enviarError(s, "TipoIncidente inválido: " + cmd.getTipoIncidente());
                    return;
                }

                simulationStateService.generarAveria(cmd.getCodigoCamion(), tipo, t);

                broadcast(new WebSocketMessage("AVERIA_GENERADA",
                        Map.of("camion", cmd.getCodigoCamion(),
                                "tipo",   tipo,
                                "momento", t)));
            }

            /* ========= 4. Control de reproducción ========= */
            case "PAUSAR_SIMULACION" -> {
                simulationStateService.pausarSimulacion();
                broadcast(new WebSocketMessage("SIMULATION_PAUSED", Map.of()));
            }

            case "REANUDAR_SIMULACION" -> {
                simulationStateService.reanudarSimulacion();
                broadcast(new WebSocketMessage("SIMULATION_RESUMED", Map.of()));
            }

            case "FINALIZAR_SIMULACION" -> {
                simulationStateService.finalizarSimulacion();
                broadcast(new WebSocketMessage("SIMULATION_ENDED", Map.of()));
            }

            /* ========= 5. Desconocido ========= */
            default -> {
                log.warn("Comando no reconocido: {}", cmd.getTipo());
                enviarError(s, "Comando no reconocido: " + cmd.getTipo());
            }
        }
    }

    /* ══════════════════════  STREAM DE POSICIONES  ══════════════════════ */

    @Scheduled(fixedRate = 1000)
    private void broadcastPosiciones() {
        if (activeSessions.isEmpty() || simulador.getFlota().isEmpty()) return;

        for (Camion c : simulador.getFlota()) {
            broadcast(new WebSocketMessage("POSICION",
                    Map.of("codigoCamion", c.getCodigo(),
                            "x",             c.getUbicacionActual().getX(),
                            "y",             c.getUbicacionActual().getY(),
                            "estado",        c.getEstado().name(),
                            "ts",            LocalDateTime.now())));
        }
    }

    /* ══════════════════════  UTILIDADES  ══════════════════════ */

    private void enviarMensaje(WebSocketSession s, WebSocketMessage m) {
        try {
            if (s.isOpen())
                s.sendMessage(new TextMessage(mapper.writeValueAsString(m)));
        } catch (Exception ex) {
            log.error("Falló envío a {}: {}", s.getId(), ex.getMessage());
        }
    }

    private void enviarError(WebSocketSession s, String msg) {
        enviarMensaje(s, new WebSocketMessage("ERROR",
                Map.of("mensaje", msg, "timestamp", LocalDateTime.now())));
    }

    private void broadcast(WebSocketMessage m) {
        activeSessions.values().forEach(s -> enviarMensaje(s, m));
    }
}
