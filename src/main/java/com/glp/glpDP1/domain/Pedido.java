package com.glp.glpDP1.domain;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un pedido de GLP por parte de un cliente
 */
@Getter
public class Pedido {
    private final String id;
    private final String idCliente;
    private final Ubicacion ubicacion;
    private final double cantidadGLP; // en m3
    private final LocalDateTime horaRecepcion;
    private final Duration tiempoLimiteEntrega;
    private LocalDateTime horaEntregaProgramada;
    private LocalDateTime horaEntregaReal;
    private String camionAsignado;
    private boolean entregado;

    public Pedido(String idCliente, Ubicacion ubicacion, double cantidadGLP,
                  LocalDateTime horaRecepcion, int horasLimiteEntrega) {
        this.id = UUID.randomUUID().toString();
        this.idCliente = idCliente;
        this.ubicacion = ubicacion;
        this.cantidadGLP = cantidadGLP;
        this.horaRecepcion = horaRecepcion;
        this.tiempoLimiteEntrega = Duration.ofHours(horasLimiteEntrega);
        this.entregado = false;
    }

    public Pedido(String idPedido, String idCliente, Ubicacion ubicacion, double cantidadGLP,
                  LocalDateTime horaRecepcion, int horasLimiteEntrega) {
        this.id = idPedido;
        this.idCliente = idCliente;
        this.ubicacion = ubicacion;
        this.cantidadGLP = cantidadGLP;
        this.horaRecepcion = horaRecepcion;
        this.tiempoLimiteEntrega = Duration.ofHours(horasLimiteEntrega);
        this.entregado = false;
    }

    // Constructor para pedidos desde archivo
    public Pedido(String idCliente, Ubicacion ubicacion, double cantidadGLP,
                  String momentoStr, int horasLimiteEntrega) {
        this.id = UUID.randomUUID().toString();
        this.idCliente = idCliente;
        this.ubicacion = ubicacion;
        this.cantidadGLP = cantidadGLP;

        // Parsear el formato "##d##h##m" a LocalDateTime
        String[] parts = momentoStr.split("d|h|m");
        int dia = Integer.parseInt(parts[0]);
        int hora = Integer.parseInt(parts[1]);
        int minuto = Integer.parseInt(parts[2]);

        // Asumimos que se refiere al día del mes actual
        LocalDateTime ahora = LocalDateTime.now();
        this.horaRecepcion = ahora
                .withDayOfMonth(dia)
                .withHour(hora)
                .withMinute(minuto)
                .withSecond(0)
                .withNano(0);

        this.tiempoLimiteEntrega = Duration.ofHours(horasLimiteEntrega);
        this.entregado = false;
    }

    public LocalDateTime getHoraLimiteEntrega() {
        return horaRecepcion.plus(tiempoLimiteEntrega);
    }

    public void setHoraEntregaProgramada(LocalDateTime horaEntregaProgramada) {
        this.horaEntregaProgramada = horaEntregaProgramada;
    }

    public void setHoraEntregaReal(LocalDateTime horaEntregaReal) {
        this.horaEntregaReal = horaEntregaReal;
        if (horaEntregaReal != null) {
            this.entregado = true;
        }
    }

    public void setCamionAsignado(String camionAsignado) {
        this.camionAsignado = camionAsignado;
    }

    /**
     * Calcula si el pedido está retrasado en el momento actual
     * @param momentoActual Momento para la verificación
     * @return true si el pedido está retrasado
     */
    public boolean estaRetrasado(LocalDateTime momentoActual) {
        return momentoActual.isAfter(getHoraLimiteEntrega());
    }

    /**
     * Calcula el tiempo restante hasta la hora límite
     * @param momentoActual Momento para el cálculo
     * @return Duración restante
     */
    public Duration tiempoRestante(LocalDateTime momentoActual) {
        return Duration.between(momentoActual, getHoraLimiteEntrega());
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "id='" + id + '\'' +
                ", cliente='" + idCliente + '\'' +
                ", ubicacion=" + ubicacion +
                ", cantidadGLP=" + cantidadGLP + "m³" +
                ", recepcion=" + horaRecepcion +
                ", limiteEntrega=" + getHoraLimiteEntrega() +
                ", entregado=" + entregado +
                '}';
    }
}
