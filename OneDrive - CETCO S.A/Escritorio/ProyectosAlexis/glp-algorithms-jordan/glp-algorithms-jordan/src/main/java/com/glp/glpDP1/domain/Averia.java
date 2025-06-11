package main.java.com.glp.glpDP1.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Representa una avería ocurrida en un camión cisterna.
 */
@Getter
@Setter
@Slf4j
public class Averia {

    private int id;  // Id de la avería
    private String turno;  // Turno en el que ocurrió la avería (T1, T2, T3)
    private String nombreCamion;  // Nombre del camión que sufrió la avería
    private String tipoIncidente;  // Tipo de incidente (TI1, TI2, TI3)
    private LocalDateTime fechaRegistro;  // Fecha y hora de registro de la avería

    /**
     * Constructor por defecto
     */
    public Averia() {
        this.fechaRegistro = LocalDateTime.now(); // Fecha de registro por defecto
    }

    /**
     * Constructor con parámetros
     * 
     * @param turno         Turno en que ocurrió la avería
     * @param nombreCamion  Nombre del camión afectado
     * @param tipoIncidente Tipo de incidente
     */
    public Averia(String turno, String nombreCamion, String tipoIncidente) {
        this.turno = turno;
        this.nombreCamion = nombreCamion;
        this.tipoIncidente = tipoIncidente;
        this.fechaRegistro = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Averia{" +
                "id=" + id +
                ", turno='" + turno + '\'' +
                ", nombreCamion='" + nombreCamion + '\'' +
                ", tipoIncidente='" + tipoIncidente + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                '}';
    }
}
