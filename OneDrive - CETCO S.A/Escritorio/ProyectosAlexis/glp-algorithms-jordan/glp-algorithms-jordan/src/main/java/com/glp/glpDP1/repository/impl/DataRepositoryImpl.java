package com.glp.glpDP1.repository.impl;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.repository.DataRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación en memoria del repositorio de datos
 */
@Repository
public class DataRepositoryImpl implements DataRepository {

    private Mapa mapa;
    private final Map<String, Camion> camiones = new ConcurrentHashMap<>();
    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<Bloqueo> bloqueos = new ArrayList<>();

    @Override
    public void guardarMapa(Mapa mapa) {
        this.mapa = mapa;
    }

    @Override
    public Mapa obtenerMapa() {
        return mapa;
    }

    @Override
    public void guardarCamiones(List<Camion> camiones) {
        camiones.forEach(camion -> this.camiones.put(camion.getCodigo(), camion));
    }

    @Override
    public List<Camion> obtenerCamiones() {
        return new ArrayList<>(camiones.values());
    }

    @Override
    public Camion buscarCamion(String codigo) {
        return camiones.get(codigo);
    }

    @Override
    public void guardarPedidos(List<Pedido> pedidos) {
        this.pedidos.clear();
        this.pedidos.addAll(pedidos);
    }

    @Override
    public List<Pedido> obtenerPedidos() {
        return new ArrayList<>(pedidos);
    }

    @Override
    public void guardarBloqueos(List<Bloqueo> bloqueos) {
        this.bloqueos.clear();
        this.bloqueos.addAll(bloqueos);

        // Actualizar bloqueos en el mapa
        if (mapa != null) {
            mapa.setBloqueos(new ArrayList<>(bloqueos));
        }
    }

    @Override
    public List<Bloqueo> obtenerBloqueos() {
        return new ArrayList<>(bloqueos);
    }
}