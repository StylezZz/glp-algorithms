package main.java.com.glp.glpDP1.services;

import com.glp.glpDP1.persistence.entity.AveriaEntity;
import com.glp.glpDP1.persistence.repository.AveriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AveriaService {

    @Autowired
    private AveriaRepository averiaRepository;

    public List<AveriaEntity> getAllAverias() {
        return averiaRepository.findAll();
    }

    public Optional<AveriaEntity> getAveriaById(int id) {
        return averiaRepository.findById(id);
    }

    public AveriaEntity createAveria(AveriaEntity averiaEntity) {
        return averiaRepository.save(averiaEntity);
    }

    public AveriaEntity updateAveria(int id, AveriaEntity updatedAveria) {
        if (averiaRepository.existsById(id)) {
            updatedAveria.setId(id); // Set the existing ID to the updated entity
            return averiaRepository.save(updatedAveria);
        }
        return null;
    }

    public void deleteAveria(int id) {
        averiaRepository.deleteById(id);
    }
}
