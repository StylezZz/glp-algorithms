package main.java.com.glp.glpDP1.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "averia")
@Getter
@Setter
@NoArgsConstructor
public class AveriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;  // Id de la avería

    @Column(name = "turno", nullable = false)
    private String turno;  // Turno en el que ocurrió la avería (T1, T2, T3)

    @Column(name = "nombre_camion", nullable = false)
    private String nombreCamion;  // Nombre del camión que sufrió la avería

    @Column(name = "tipo_incidente", nullable = false)
    private String tipoIncidente;  // Tipo de incidente (TI1, TI2, TI3)

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;  // Fecha y hora de registro de la avería

    // Constructor adicional
    public AveriaEntity(String turno, String nombreCamion, String tipoIncidente, LocalDateTime fechaRegistro) {
        this.turno = turno;
        this.nombreCamion = nombreCamion;
        this.tipoIncidente = tipoIncidente;
        this.fechaRegistro = fechaRegistro;
    }
}
