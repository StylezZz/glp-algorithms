package com.glp.glpDP1.persistence.mapper;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.persistence.entity.BloqueoEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BloqueoMapper {

    /**
     * Convierte una lista de entidades BloqueoEntity (mismo grupo) en un Bloqueo de dominio
     * @param entities lista de entidades con el mismo flag_mes_agrupa
     * @return objeto Bloqueo con nodos agrupados y rango de tiempo
     */
    public static Bloqueo toDomain(List<BloqueoEntity> entities) {
        if (entities == null || entities.isEmpty()) return null;

        BloqueoEntity ref = entities.get(0);

        // Suponiendo que todos son del mismo mes y año fijo
        LocalDateTime horaInicio = LocalDateTime.of(2025, 6, ref.getDiaInicio(), ref.getHoraInicio(), ref.getMinutoInicio());
        LocalDateTime horaFin = LocalDateTime.of(2025, 6, ref.getDiaFin(), ref.getHoraFin(), ref.getMinutoFin());

        List<Ubicacion> nodos = new ArrayList<>();
        for (BloqueoEntity e : entities) {
            nodos.add(new Ubicacion(e.getUbicacionX(), e.getUbicacionY()));
        }

        return new Bloqueo(horaInicio, horaFin, nodos);
    }
}
