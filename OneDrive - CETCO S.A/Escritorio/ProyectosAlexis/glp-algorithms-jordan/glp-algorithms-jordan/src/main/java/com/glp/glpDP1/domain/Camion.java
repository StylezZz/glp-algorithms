package com.glp.glpDP1.domain;

import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.TipoCamion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Representa un camión cisterna para la distribución de GLP
 */
@Getter
@Setter
@Slf4j
public class Camion {
    private final String codigo;
    private final TipoCamion tipo;
    private final double capacidadTanqueGLP; // en m3
    private final double pesoTara; // en toneladas
    private final double capacidadTanqueCombustible; // en galones
    private final double velocidadPromedio; // en km/h

    private Ubicacion ubicacionActual;
    private EstadoCamion estado;
    private double nivelGLPActual; // en m3
    private double nivelCombustibleActual; // en galones
    private LocalDateTime fechaUltimoMantenimiento;
    private LocalDateTime fechaProximoMantenimiento;
    private boolean enMantenimiento;
    private boolean averiado;
    private TipoIncidente tipoAveriaActual;
    private LocalDateTime horaFinInmovilizacion;
    private LocalDateTime horaDisponibilidad;

    public Camion(String codigo, TipoCamion tipo, Ubicacion ubicacionInicial) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.ubicacionActual = ubicacionInicial;
        this.estado = EstadoCamion.DISPONIBLE;

        // Inicializar según el tipo de camión
        switch (tipo) {
            case TA:
                this.capacidadTanqueGLP = 25.0;
                this.pesoTara = 2.5;
                break;
            case TB:
                this.capacidadTanqueGLP = 15.0;
                this.pesoTara = 2.0;
                break;
            case TC:
                this.capacidadTanqueGLP = 10.0;
                this.pesoTara = 1.5;
                break;
            case TD:
                this.capacidadTanqueGLP = 5.0;
                this.pesoTara = 1.0;
                break;
            default:
                throw new IllegalArgumentException("Tipo de camión no válido");
        }

        this.capacidadTanqueCombustible = 25.0; // 25 galones para todos
        this.velocidadPromedio = 50.0; // 50 km/h para todos
        this.nivelGLPActual = 0.0; // Inicia vacío de GLP
        this.nivelCombustibleActual = capacidadTanqueCombustible; // Inicia con tanque lleno de combustible
        this.fechaUltimoMantenimiento = LocalDateTime.now().minusDays(30); // Suponemos último mantenimiento hace 30
                                                                           // días
        this.enMantenimiento = false;
        this.averiado = false;
    }

    /**
     * Calcula el peso total actual del camión (tara + carga)
     * 
     * @return Peso en toneladas
     */
    public double calcularPesoTotal() {
        // Densidad del GLP = 0.5 ton/m3
        double pesoGLP = nivelGLPActual * 0.5;
        return pesoTara + pesoGLP;
    }

    /**
     * Calcula el consumo de combustible para una distancia dada
     * 
     * @param distanciaKm Distancia a recorrer en km
     * @return Consumo en galones
     */
    public double calcularConsumoCombustible(double distanciaKm) {
        double pesoTotal = calcularPesoTotal();
        return (distanciaKm * pesoTotal) / 180.0;
    }

    /**
     * Calcula la distancia máxima que puede recorrer con el combustible actual
     * 
     * @return Distancia en km
     */
    public double calcularDistanciaMaxima() {
        double pesoTotal = calcularPesoTotal();
        if (pesoTotal <= 0) {
            return 0;
        }
        return (nivelCombustibleActual * 180.0) / pesoTotal;
    }

    /**
     * Carga GLP al camión
     * 
     * @param cantidad Cantidad a cargar en m3
     * @return true si se pudo cargar, false si excede capacidad
     */
    public boolean cargarGLP(double cantidad) {
        if (nivelGLPActual + cantidad <= capacidadTanqueGLP) {
            nivelGLPActual += cantidad;
            return true;
        }
        return false;
    }

    /**
     * Descarga GLP del camión
     * 
     * @param cantidad Cantidad a descargar en m3
     * @return true si se pudo descargar, false si no hay suficiente
     */
    public boolean descargarGLP(double cantidad) {
        if (cantidad <= nivelGLPActual) {
            nivelGLPActual -= cantidad;
            return true;
        }
        return false;
    }

    /**
     * Recargar combustible al tanque del camión
     * 
     * @param cantidad Cantidad a recargar en galones
     * @return true si se pudo recargar, false si excede capacidad
     */
    public boolean recargarCombustible(double cantidad) {
        if (nivelCombustibleActual + cantidad <= capacidadTanqueCombustible) {
            nivelCombustibleActual += cantidad;
            return true;
        }
        return false;
    }

    /**
     * Consume combustible al recorrer una distancia
     * 
     * @param distanciaKm Distancia recorrida en km
     * @return true si tenía suficiente combustible, false si no
     */
    public boolean consumirCombustible(double distanciaKm) {
        double consumo = calcularConsumoCombustible(distanciaKm);
        if (consumo <= nivelCombustibleActual) {
            nivelCombustibleActual -= consumo;
            return true;
        }
        return false;
    }

    /**
     * Registra una avería en el camión
     * 
     * @param tipo          Tipo de incidente
     * @param momentoActual Momento en que ocurre la avería
     */
    public void registrarAveria(TipoIncidente tipo, LocalDateTime momentoActual) {
        this.averiado = true;
        this.tipoAveriaActual = tipo;
        this.estado = EstadoCamion.AVERIADO;

        Turno turnoActual = Turno.obtenerTurnoPorHora(momentoActual.getHour());

        // Calcular tiempo de inmovilización según el tipo de incidente
        LocalDateTime finInmovilizacion;
        LocalDateTime disponibilidad;

        switch (tipo) {
            case TI1:
                // 2 horas inmovilizado, luego disponible
                finInmovilizacion = momentoActual.plusHours(2);
                disponibilidad = finInmovilizacion; // Disponible inmediatamente después de la inmovilización
                log.info("Avería TI1 en camión {}: Inmovilizado por 2h en ubicación {}", codigo, finInmovilizacion,
                        ubicacionActual);
                log.info("  - Momento de avería: {}", momentoActual);
                log.info("  - Fin de inmovilización: {}", finInmovilizacion);
                log.info("  - Disponible nuevamente: {}", disponibilidad);
                break;
            case TI2:
                // 2 horas inmovilizado, luego indisponible por un turno
                finInmovilizacion = momentoActual.plusHours(2);

                // Calcular disponibilidad según el turno actual
                if (turnoActual == Turno.T1) {
                    // Turno 1 (00:00-08:00): disponible en turno 3 (16:00-00:00)
                    disponibilidad = momentoActual.withHour(16).withMinute(0).withSecond(0);
                    if (disponibilidad.isBefore(momentoActual)) {
                        disponibilidad = disponibilidad.plusDays(1); // Asegurar que sea futuro
                    }
                } else if (turnoActual == Turno.T2) {
                    // Turno 2 (08:00-16:00): disponible en turno 1 del día siguiente (00:00-08:00)
                    disponibilidad = momentoActual.plusDays(1).withHour(0).withMinute(0).withSecond(0);
                } else {
                    // Turno 3 (16:00-00:00): disponible en turno 2 del día siguiente (08:00-16:00)
                    disponibilidad = momentoActual.plusDays(1).withHour(8).withMinute(0).withSecond(0);
                }

                log.info("Avería TI2 en camión {}: Inmovilizado por 2h en ubicación {}, luego taller",
                        codigo, ubicacionActual);
                log.info("  - Momento de avería: {} (Turno {})", momentoActual, turnoActual);
                log.info("  - Fin inmovilización: {}", finInmovilizacion);
                log.info("  - Disponible nuevamente: {}", disponibilidad);
                break;
            case TI3:
                // 4 horas inmovilizado, luego indisponible por 3 días
                finInmovilizacion = momentoActual.plusHours(4);
                disponibilidad = momentoActual.plusDays(3).withHour(0).withMinute(0).withSecond(0);
                log.info("Avería TI3 en camión {}: Inmovilizado por 4h en ubicación {}, luego taller 3 días",
                        codigo, ubicacionActual);
                log.info("  - Momento de avería: {}", momentoActual);
                log.info("  - Fin inmovilización: {}", finInmovilizacion);
                log.info("  - Disponible nuevamente: {}", disponibilidad);
                break;
            default:
                throw new IllegalArgumentException("Tipo de incidente no válido");
        }

        this.horaFinInmovilizacion = finInmovilizacion;
        this.horaDisponibilidad = disponibilidad;
    }

    /**
     * Actualiza el estado del camión según el momento actual
     * 
     * @param momentoActual Momento para la actualización
     */
    public void actualizarEstado(LocalDateTime momentoActual) {
        String estadoAnterior = this.estado.toString();

        // Verificar si está en mantenimiento
        if (enMantenimiento) {
            // Verificar si terminó el mantenimiento (dura exactamente 24 horas)
            LocalDateTime finMantenimiento = fechaUltimoMantenimiento.plusHours(24);
            if (momentoActual.isEqual(finMantenimiento) || momentoActual.isAfter(finMantenimiento)) {
                enMantenimiento = false;
                estado = EstadoCamion.DISPONIBLE;
                log.info("Camión {} finaliza mantenimiento en {} y queda DISPONIBLE", codigo, momentoActual);
            }
            return;
        }

        // Verificar si debe entrar en mantenimiento
        if (fechaProximoMantenimiento != null) {
            // La fecha de mantenimiento debe ser hoy y el camión no debe estar ya en
            // mantenimiento
            if (momentoActual.toLocalDate().isEqual(fechaProximoMantenimiento.toLocalDate()) &&
                    !enMantenimiento && (estado == EstadoCamion.DISPONIBLE || estado == EstadoCamion.EN_RUTA)) {

                enMantenimiento = true;
                estado = EstadoCamion.EN_MANTENIMIENTO;
                fechaUltimoMantenimiento = momentoActual;
                log.info("Camión {} entra en MANTENIMIENTO en {}, saldrá en {}",
                        codigo, momentoActual, momentoActual.plusHours(24));
                return;
            }
        }

        // Verificar si está averiado
        if (averiado) {
            // Verificar si terminó la inmovilización
            if (horaFinInmovilizacion != null && momentoActual.isAfter(horaFinInmovilizacion)) {
                log.info("Camión {} finaliza INMOVILIZACIÓN por avería en {}", codigo, momentoActual);

                // Ya no está inmovilizado, pero puede seguir indisponible
                if (horaDisponibilidad != null && momentoActual.isAfter(horaDisponibilidad)) {
                    // Ya está disponible nuevamente
                    averiado = false;
                    tipoAveriaActual = null;
                    estado = EstadoCamion.DISPONIBLE;
                    log.info("Camión {} se recupera de avería y queda DISPONIBLE en {}", codigo, momentoActual);
                } else {
                    // No inmovilizado pero indisponible (en taller)
                    estado = EstadoCamion.INDISPONIBLE;
                    log.info("Camión {} pasa a estado INDISPONIBLE (en taller) hasta {}",
                            codigo, horaDisponibilidad);
                }
            } else {
                // Sigue inmovilizado en el lugar de la avería
                estado = EstadoCamion.AVERIADO;
                if (horaFinInmovilizacion != null) {
                    log.debug("Camión {} sigue AVERIADO hasta {}", codigo, horaFinInmovilizacion);
                }
            }
        }

        // Loguear cambio de estado si ocurrió
        if (!estadoAnterior.equals(this.estado.toString())) {
            log.info("Camión {} cambió de estado: {} -> {}", codigo, estadoAnterior, this.estado);
        }
    }

    /**
     * Calcula el tiempo estimado de viaje entre dos ubicaciones
     * 
     * @param origen  Ubicación de origen
     * @param destino Ubicación de destino
     * @return Duración estimada del viaje
     */
    public Duration calcularTiempoViaje(Ubicacion origen, Ubicacion destino) {
        int distancia = origen.distanciaA(destino);
        double horasViaje = distancia / velocidadPromedio;
        long minutosViaje = Math.round(horasViaje * 60);
        return Duration.ofMinutes(minutosViaje);
    }

    @Override
    public String toString() {
        return "Camion{" +
                "codigo='" + codigo + '\'' +
                ", tipo=" + tipo +
                ", ubicacion=" + ubicacionActual +
                ", estado=" + estado +
                ", GLP=" + String.format("%.2f", nivelGLPActual) + "m³" +
                ", combustible=" + String.format("%.2f", nivelCombustibleActual) + "gal" +
                '}';
    }
}
