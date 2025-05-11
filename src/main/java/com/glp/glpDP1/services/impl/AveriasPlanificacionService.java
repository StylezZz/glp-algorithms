package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Camion;
import com.glp.glpDP1.domain.EventoRuta;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;
import com.glp.glpDP1.repository.impl.DataRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para planificación y monitoreo de averías
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AveriasPlanificacionService {

    private final AveriaService averiaService;
    
    @Autowired
    private DataRepositoryImpl dataRepository;
    
    // Registro de eventos de avería para el dashboard
    private final List<Map<String, Object>> historialAverias = new ArrayList<>();
    
    /**
     * Registra manualmente una avería para un camión
     */
    public Map<String, Object> registrarAveriaManual(String codigoCamion, 
                                                    TipoIncidente tipoIncidente, 
                                                    LocalDateTime momento) {
        Camion camion = dataRepository.buscarCamion(codigoCamion);
        
        if (camion == null) {
            throw new IllegalArgumentException("Camión no encontrado: " + codigoCamion);
        }
        
        if (camion.getEstado() != EstadoCamion.DISPONIBLE && 
            camion.getEstado() != EstadoCamion.EN_RUTA) {
            throw new IllegalArgumentException("El camión no está disponible para registrar avería: " + 
                                              camion.getEstado());
        }
        
        // Registrar la avería en el camión
        camion.registrarAveria(tipoIncidente, momento);
        
        // Crear registro para el dashboard
        Map<String, Object> registroAveria = crearRegistroAveria(
            camion, tipoIncidente, momento, camion.getUbicacionActual(), "MANUAL");
            
        // Agregar al historial
        historialAverias.add(registroAveria);
        
        log.info("Avería manual registrada para camión {} de tipo {} en {}", 
                codigoCamion, tipoIncidente, momento);
                
        return registroAveria;
    }
    
    /**
     * Registra una avería ocurrida durante una simulación
     */
    public void registrarAveriaSimulada(Camion camion, TipoIncidente tipoIncidente, 
                                       LocalDateTime momento, Ruta ruta, int nodoIndice) {
        
        // Crear registro para el dashboard
        Map<String, Object> registroAveria = crearRegistroAveria(
            camion, tipoIncidente, momento, ruta.getSecuenciaNodos().get(nodoIndice), "SIMULACIÓN");
            
        // Agregar detalles de la ruta
        registroAveria.put("rutaId", ruta.getId());
        registroAveria.put("pedidosAsignados", ruta.getPedidosAsignados().size());
        registroAveria.put("porcentajeRuta", 
                          Math.round((double)nodoIndice / ruta.getSecuenciaNodos().size() * 100) + "%");
        
        // Agregar al historial
        historialAverias.add(registroAveria);
        
        log.info("Avería simulada registrada para camión {} en ruta {}, nodo {}/{} ({}%)", 
                camion.getCodigo(), ruta.getId(), nodoIndice, ruta.getSecuenciaNodos().size(), 
                Math.round((double)nodoIndice / ruta.getSecuenciaNodos().size() * 100));
    }
    
    /**
     * Crea un registro de avería para el dashboard
     */
    private Map<String, Object> crearRegistroAveria(Camion camion, TipoIncidente tipoIncidente, 
                                                  LocalDateTime momento, Ubicacion ubicacion, 
                                                  String origen) {
        Map<String, Object> registro = new HashMap<>();
        
        registro.put("id", UUID.randomUUID().toString());
        registro.put("codigoCamion", camion.getCodigo());
        registro.put("tipoIncidente", tipoIncidente.toString());
        registro.put("momento", momento.format(DateTimeFormatter.ISO_DATE_TIME));
        registro.put("ubicacion", ubicacion.toString());
        registro.put("turno", Turno.obtenerTurnoPorHora(momento.getHour()).toString());
        registro.put("origen", origen);
        
        // Calcular tiempos de recuperación
        registro.put("finInmovilizacion", camion.getHoraFinInmovilizacion().format(DateTimeFormatter.ISO_DATE_TIME));
        registro.put("horaDisponibilidad", camion.getHoraDisponibilidad().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Calcular duración de inmovilización
        Duration duracionInmovilizacion = Duration.between(momento, camion.getHoraFinInmovilizacion());
        registro.put("duracionInmovilizacion", duracionInmovilizacion.toHours() + "h " + 
                                             (duracionInmovilizacion.toMinutes() % 60) + "m");
        
        // Calcular duración total de indisponibilidad
        Duration duracionTotal = Duration.between(momento, camion.getHoraDisponibilidad());
        registro.put("duracionIndisponibilidad", duracionTotal.toHours() + "h " + 
                                               (duracionTotal.toMinutes() % 60) + "m");
        
        return registro;
    }
    
    /**
     * Obtiene el historial de averías para el dashboard
     */
    public List<Map<String, Object>> obtenerHistorialAverias() {
        return new ArrayList<>(historialAverias);
    }
    
    /**
     * Obtiene estadísticas de averías por tipo de incidente
     */
    public Map<String, Object> obtenerEstadisticasAverias() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Contar averías por tipo
        Map<String, Long> conteoTipos = historialAverias.stream()
            .collect(Collectors.groupingBy(
                m -> (String)m.get("tipoIncidente"), 
                Collectors.counting()));
                
        estadisticas.put("porTipo", conteoTipos);
        
        // Contar averías por turno
        Map<String, Long> conteoTurnos = historialAverias.stream()
            .collect(Collectors.groupingBy(
                m -> (String)m.get("turno"), 
                Collectors.counting()));
                
        estadisticas.put("porTurno", conteoTurnos);
        
        // Contar averías por camión
        Map<String, Long> conteoCamiones = historialAverias.stream()
            .collect(Collectors.groupingBy(
                m -> (String)m.get("codigoCamion"), 
                Collectors.counting()));
                
        estadisticas.put("porCamion", conteoCamiones);
        
        // Total de averías
        estadisticas.put("total", historialAverias.size());
        
        return estadisticas;
    }
    
    /**
     * Obtiene un informe detallado del impacto de las averías
     */
    public String generarInformeAverias() {
        StringBuilder informe = new StringBuilder();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        informe.append("# INFORME DE AVERÍAS\n\n");
        
        informe.append("## Resumen\n");
        informe.append("Total de averías registradas: ").append(historialAverias.size()).append("\n\n");
        
        // Estadísticas por tipo
        Map<String, Long> conteoTipos = historialAverias.stream()
            .collect(Collectors.groupingBy(
                m -> (String)m.get("tipoIncidente"), 
                Collectors.counting()));
                
        informe.append("## Distribución por tipo\n");
        for (Map.Entry<String, Long> entry : conteoTipos.entrySet()) {
            informe.append("- ").append(entry.getKey()).append(": ")
                   .append(entry.getValue()).append(" (")
                   .append(Math.round((double)entry.getValue() / historialAverias.size() * 100))
                   .append("%)\n");
        }
        informe.append("\n");
        
        // Estadísticas por turno
        Map<String, Long> conteoTurnos = historialAverias.stream()
            .collect(Collectors.groupingBy(
                m -> (String)m.get("turno"), 
                Collectors.counting()));
                
        informe.append("## Distribución por turno\n");
        for (Map.Entry<String, Long> entry : conteoTurnos.entrySet()) {
            informe.append("- ").append(entry.getKey()).append(": ")
                   .append(entry.getValue()).append(" (")
                   .append(Math.round((double)entry.getValue() / historialAverias.size() * 100))
                   .append("%)\n");
        }
        informe.append("\n");
        
        // Detalles de cada avería
        informe.append("## Detalles de averías\n\n");
        
        for (Map<String, Object> averia : historialAverias) {
            informe.append("### Avería en camión ").append(averia.get("codigoCamion")).append("\n");
            informe.append("- **Tipo**: ").append(averia.get("tipoIncidente")).append("\n");
            informe.append("- **Momento**: ").append(averia.get("momento")).append(" (Turno ")
                   .append(averia.get("turno")).append(")\n");
            informe.append("- **Ubicación**: ").append(averia.get("ubicacion")).append("\n");
            informe.append("- **Fin inmovilización**: ").append(averia.get("finInmovilizacion")).append(" (")
                   .append(averia.get("duracionInmovilizacion")).append(")\n");
            informe.append("- **Disponible nuevamente**: ").append(averia.get("horaDisponibilidad")).append(" (")
                   .append(averia.get("duracionIndisponibilidad")).append(" total)\n");
            informe.append("- **Origen**: ").append(averia.get("origen")).append("\n");
            
            if (averia.containsKey("rutaId")) {
                informe.append("- **Ruta**: ").append(averia.get("rutaId")).append(" (")
                       .append(averia.get("pedidosAsignados")).append(" pedidos)\n");
                informe.append("- **Progreso**: ").append(averia.get("porcentajeRuta")).append(" de la ruta\n");
            }
            
            informe.append("\n");
        }
        
        return informe.toString();
    }
    
    /**
     * Limpia el historial de averías (para reiniciar simulaciones)
     */
    public void limpiarHistorial() {
        historialAverias.clear();
        log.info("Historial de averías limpiado");
    }
}