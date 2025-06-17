package com.glp.glpDP1.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

//@Service
@RequiredArgsConstructor
public class SimulacionPublisher {

    private final SimuladorEntregas simulador;      // ✅ ya es bean
    private final SimpMessagingTemplate broker;     // también lo inyecta Spring

    @Scheduled(fixedRate = 1000)                    // cada 1 s
    public void broadcastPosiciones() {

        simulador.getFlota().forEach(camion ->      // método generado por @Getter
                broker.convertAndSend("/topic/vehiculos", Map.of(
                        "tipo",   "POSICION",
                        "codigoCamion", camion.getCodigo(),
                        "x",      camion.getUbicacionActual().getX(),
                        "y",      camion.getUbicacionActual().getY(),
                        "estado", camion.getEstado().name(),
                        "ts",     LocalDateTime.now().toString()
                ))
        );
    }
}
