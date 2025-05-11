package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Almacen;
import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Mapa;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.domain.enums.TipoCamion;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.InitService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitServiceImpl implements InitService {

    private final DataRepository dataRepository;

    private static final Pattern MANT_PATTERN = Pattern.compile("(\\d{8}):(\\w{2})(\\d{2})");

    @PostConstruct
    public void init() {
        log.info("Inicializando datos de la aplicación...");
        inicializarDatos();
    }

    @Override
    public void inicializarDatos() {
        // Inicializar mapa
        Mapa mapa = new Mapa();
        dataRepository.guardarMapa(mapa);
        log.info("Mapa inicializado: {}x{}", mapa.getAncho(), mapa.getAlto());

        // Inicializar camiones según la flota definida en el documento
        List<Camion> camiones = new ArrayList<>();

        // Tipo TA: 2 unidades
        camiones.add(new Camion("TA01", TipoCamion.TA, mapa.obtenerAlmacenCentral().getUbicacion()));
        camiones.add(new Camion("TA02", TipoCamion.TA, mapa.obtenerAlmacenCentral().getUbicacion()));

        // Tipo TB: 4 unidades
        for (int i = 1; i <= 4; i++) {
            String codigo = "TB0" + i;
            camiones.add(new Camion(codigo, TipoCamion.TB, mapa.obtenerAlmacenCentral().getUbicacion()));
        }

        // Tipo TC: 4 unidades
        for (int i = 1; i <= 4; i++) {
            String codigo = "TC0" + i;
            camiones.add(new Camion(codigo, TipoCamion.TC, mapa.obtenerAlmacenCentral().getUbicacion()));
        }

        // Tipo TD: 10 unidades
        for (int i = 1; i <= 10; i++) {
            String codigo = "TD" + (i < 10 ? "0" + i : i);
            camiones.add(new Camion(codigo, TipoCamion.TD, mapa.obtenerAlmacenCentral().getUbicacion()));
        }

        // Guardar camiones en el repositorio
        dataRepository.guardarCamiones(camiones);
        log.info("Camiones inicializados: {} unidades", camiones.size());

        // Los almacenes ya están inicializados en el mapa
        List<Almacen> almacenes = new ArrayList<>(mapa.getAlmacenes());
        log.info("Almacenes inicializados: {} unidades", almacenes.size());
    }

    @Override
    public List<Camion> cargarCamiones(InputStream inputStream) {
        List<Camion> camiones = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Formato esperado: CODIGO,TIPO,X,Y
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        String codigo = parts[0];
                        TipoCamion tipo = TipoCamion.valueOf(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);

                        Ubicacion ubicacion = new Ubicacion(x, y);
                        Camion camion = new Camion(codigo, tipo, ubicacion);

                        camiones.add(camion);
                    }
                } catch (Exception e) {
                    log.error("Error al parsear línea de camión: {}", line, e);
                }
            }

            // Actualizar camiones en el repositorio
            if (!camiones.isEmpty()) {
                dataRepository.guardarCamiones(camiones);
            }

            log.info("Cargados {} camiones desde archivo", camiones.size());

        } catch (IOException e) {
            log.error("Error al leer archivo de camiones", e);
            throw new RuntimeException("Error al leer archivo de camiones", e);
        }

        return camiones;
    }

    @Override
    public int cargarPlanMantenimiento(InputStream inputStream) {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // Formato esperado: aaaammdd:TTNN
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String fechaStr = parts[0];
                        String codigoCamion = parts[1];
                        System.out.println("Fecha: " + fechaStr + " Camion: " + codigoCamion);
                        
                        // Modificado: Primero parsear a LocalDate y luego convertir a LocalDateTime
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                        LocalDate fecha = LocalDate.parse(fechaStr, formatter);
                        LocalDateTime fechaDateTime = fecha.atStartOfDay();

                        // Buscar camión y programar mantenimiento
                        Camion camion = dataRepository.buscarCamion(codigoCamion);
                        if (camion != null) {
                            camion.setFechaProximoMantenimiento(fechaDateTime);
                            count++;
                        } else {
                            log.warn("Camión no encontrado para mantenimiento: {}", codigoCamion);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error al parsear línea de mantenimiento: {}", line, e);
                }
            }

            log.info("Cargados {} registros de mantenimiento", count);

        } catch (IOException e) {
            log.error("Error al leer archivo de mantenimiento", e);
            throw new RuntimeException("Error al leer archivo de mantenimiento", e);
        }

        return count;
    }

    @Override
    public Mapa obtenerMapa() {
        return dataRepository.obtenerMapa();
    }

    @Override
    public List<Camion> obtenerCamiones() {
        return dataRepository.obtenerCamiones();
    }

    @Override
    public List<Almacen> obtenerAlmacenes() {
        return dataRepository.obtenerMapa().getAlmacenes();
    }
}