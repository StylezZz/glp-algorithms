package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.services.MapaService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class MapaServiceImpl implements MapaService {

    // Como no tenemos persistencia de datos, mantenemos el mapa en memoria
    private Mapa mapaActual;

    @PostConstruct
    public void init() {
        // Inicializar el mapa con valores predeterminados
        mapaActual = new Mapa();
        log.info("Mapa inicializado: {}x{}", mapaActual.getAncho(), mapaActual.getAlto());
    }

    @Override
    public Mapa obtenerMapa() {
        return mapaActual;
    }

    @Override
    public Mapa actualizarBloqueos(List<Bloqueo> bloqueos) {
        mapaActual.setBloqueos(bloqueos);
        log.info("Bloqueos actualizados en el mapa: {} bloqueos", bloqueos.size());
        return mapaActual;
    }

    @Override
    public Mapa agregarBloqueo(Bloqueo bloqueo) {
        mapaActual.agregarBloqueo(bloqueo);
        log.info("Bloqueo agregado al mapa: {}", bloqueo.getId());
        return mapaActual;
    }
}