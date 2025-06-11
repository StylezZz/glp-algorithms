package main.java.com.glp.glpDP1.persistence.mapper;

import com.glp.glpDP1.domain.Averia;
import com.glp.glpDP1.persistence.entity.AveriaEntity;

public class AveriaMapper {

    // Convierte de AveriaEntity a Averia
    public static Averia toDomain(AveriaEntity entity) {
        if (entity == null) {
            return null;
        }
        Averia averia = new Averia();
        averia.setId(entity.getId());
        averia.setTurno(entity.getTurno());
        averia.setNombreCamion(entity.getNombreCamion());
        averia.setTipoIncidente(entity.getTipoIncidente());
        averia.setFechaRegistro(entity.getFechaRegistro());
        return averia;
    }

    // Convierte de Averia a AveriaEntity
    public static AveriaEntity toEntity(Averia domain) {
        if (domain == null) {
            return null;
        }
        AveriaEntity entity = new AveriaEntity();
        entity.setId(domain.getId());
        entity.setTurno(domain.getTurno());
        entity.setNombreCamion(domain.getNombreCamion());
        entity.setTipoIncidente(domain.getTipoIncidente());
        entity.setFechaRegistro(domain.getFechaRegistro());
        return entity;
    }
}
