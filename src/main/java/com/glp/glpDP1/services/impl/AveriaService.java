package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio para gestionar las averías de los camiones
 */
@Service
@Slf4j
public class AveriaService {

    // Mapeo de averías registradas por turno y camión
    private final Map<String, TipoIncidente> averiasRegistradas = new HashMap<>();
    
    // Para seguimiento de averías ya ocurridas por día y camión
    private final Set<String> averiasOcurridas = new HashSet<>();

    /**
     * Carga las averías desde un archivo
     * @param inputStream Stream del archivo de averías
     * @return Número de averías cargadas
     */
    public int cargarAverias(InputStream inputStream) {
        int count = 0;
        averiasRegistradas.clear(); // Limpiar registros anteriores
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Saltar líneas vacías
                }
                
                try {
                    // Formato: tt_######_ti (T1_TA01_TI1)
                    String[] parts = line.split("_");
                    if (parts.length == 3) {
                        Turno turno;
                        try {
                            turno = Turno.valueOf(parts[0]);
                        } catch (IllegalArgumentException e) {
                            log.error("Turno inválido en línea: {}", line);
                            continue;
                        }
                        
                        String codigoCamion = parts[1];
                        
                        TipoIncidente tipoIncidente;
                        try {
                            tipoIncidente = TipoIncidente.valueOf(parts[2]);
                        } catch (IllegalArgumentException e) {
                            log.error("Tipo de incidente inválido en línea: {}", line);
                            continue;
                        }
                        
                        // Clave compuesta: Turno_Camión
                        String key = turno + "_" + codigoCamion;
                        
                        // Registrar avería
                        averiasRegistradas.put(key, tipoIncidente);
                        count++;
                        log.info("Avería registrada: {}, {}, {}", turno, codigoCamion, tipoIncidente);
                    } else {
                        log.warn("Formato incorrecto en línea: {}", line);
                    }
                } catch (Exception e) {
                    log.error("Error al parsear línea de avería: {}", line, e);
                }
            }
        } catch (IOException e) {
            log.error("Error al leer archivo de averías", e);
            throw new RuntimeException("Error al leer archivo de averías", e);
        }
        
        log.info("Total de averías cargadas: {}", count);
        return count;
    }

    /**
     * Verifica si un camión tiene programada una avería para el turno actual
     */
    public boolean tieneProgramadaAveria(String codigoCamion, LocalDateTime momento) {
        Turno turnoActual = Turno.obtenerTurnoPorHora(momento.getHour());
        String key = turnoActual + "_" + codigoCamion;
        
        // Verificar si ya se procesó esta avería hoy
        String averiaDiaClave = momento.toLocalDate() + "_" + key;
        if (averiasOcurridas.contains(averiaDiaClave)) {
            return false; // Ya se procesó esta avería hoy
        }
        
        return averiasRegistradas.containsKey(key);
    }
    
    /**
     * Obtiene el tipo de incidente programado para un camión en un turno específico
     * @param codigoCamion Código del camión
     * @param turno Turno a verificar
     * @return El tipo de incidente, o null si no hay avería programada
     */
    public TipoIncidente obtenerIncidenteProgramado(String codigoCamion, Turno turno) {
        String key = turno + "_" + codigoCamion;
        return averiasRegistradas.get(key);
    }
    
    /**
     * Registra que una avería ya ocurrió para evitar duplicados
     */
    public void registrarAveriaOcurrida(String codigoCamion, LocalDateTime momento) {
        Turno turnoActual = Turno.obtenerTurnoPorHora(momento.getHour());
        String key = turnoActual + "_" + codigoCamion;
        String averiaDiaClave = momento.toLocalDate() + "_" + key;
        averiasOcurridas.add(averiaDiaClave);
        log.info("Avería registrada como ocurrida: {}", averiaDiaClave);
    }

    /**
     * Limpia las averías registradas (útil para reiniciar simulaciones)
     */
    public void limpiarAverias() {
        averiasRegistradas.clear();
        averiasOcurridas.clear();
        log.info("Registro de averías limpiado");
    }
    
    /**
     * Lista todas las averías programadas (útil para diagnóstico)
     */
    public List<Map<String, String>> listarAveriasProgramadas() {
        List<Map<String, String>> resultado = new ArrayList<>();
        
        for (Map.Entry<String, TipoIncidente> entry : averiasRegistradas.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length == 2) {
                Map<String, String> averia = new HashMap<>();
                averia.put("turno", parts[0]);
                averia.put("camion", parts[1]);
                averia.put("tipoIncidente", entry.getValue().toString());
                resultado.add(averia);
            }
        }
        
        return resultado;
    }

    /**
     * Persiste una avería que acaba de ocurrir (WebSocket, REST, test…).
     * Si ya existe una avería programada para el mismo turno y camión la reemplaza,
     * pues significa que acaba de suceder.
     */
    public void registrarAveria(String codigoCamion,
                                TipoIncidente tipoIncidente,
                                LocalDateTime momento) {

        Turno turnoActual = Turno.obtenerTurnoPorHora(momento.getHour());
        String key = turnoActual + "_" + codigoCamion;

        averiasRegistradas.put(key, tipoIncidente);      // sobre-escribe o crea entrada
        registrarAveriaOcurrida(codigoCamion, momento);  // marcar como atendida hoy
        log.info("Avería guardada: {} – {} – {}", turnoActual, codigoCamion, tipoIncidente);
    }


}