package main.java.com.glp.glpDP1.repository;

import com.glp.glpDP1.persistence.entity.AveriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AveriaRepository extends JpaRepository<AveriaEntity, Integer> {
    // Aquí puedes definir métodos personalizados según las necesidades de tu aplicación
    List<AveriaEntity> findByTurno(String turno); // Ejemplo: buscar averías por turno
    List<AveriaEntity> findByNombreCamion(String nombreCamion); // Ejemplo: buscar averías por nombre del camión
}
