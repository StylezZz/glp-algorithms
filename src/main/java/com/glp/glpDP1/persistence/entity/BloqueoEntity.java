package com.glp.glpDP1.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bloqueo")
@Getter
@Setter
@NoArgsConstructor
public class BloqueoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "dia_inicio")
    private int diaInicio;

    @Column(name = "hora_inicio")
    private int horaInicio;

    @Column(name = "minuto_inicio")
    private int minutoInicio;

    @Column(name = "dia_fin")
    private int diaFin;

    @Column(name = "hora_fin")
    private int horaFin;

    @Column(name = "minuto_fin")
    private int minutoFin;

    @Column(name = "ubicacion_x")
    private int ubicacionX;

    @Column(name = "ubicacion_y")
    private int ubicacionY;

    @Column(name = "flag_mes_agrupa")
    private int flagMesAgrupa = 0;

    @Column(name = "flag_interno_agrupa")
    private int flagInternoAgrupa = 0;
}
