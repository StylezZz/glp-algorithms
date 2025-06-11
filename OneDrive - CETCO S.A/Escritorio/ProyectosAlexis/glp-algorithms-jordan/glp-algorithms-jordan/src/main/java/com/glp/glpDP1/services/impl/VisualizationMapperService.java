package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.api.dto.response.RutaVisualizacionDTO;
import com.glp.glpDP1.api.dto.response.SimulacionSemanalResponseDTO;
import com.glp.glpDP1.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para mapear objetos del dominio a DTOs de visualización
 */
@Service
public class VisualizationMapperService {

    /**
     * Convierte una Ruta del dominio a RutaVisualizacionDTO
     */
    public RutaVisualizacionDTO mapRutaToDTO(Ruta ruta) {
        if (ruta == null) return null;

        RutaVisualizacionDTO dto = new RutaVisualizacionDTO();
        dto.setId(ruta.getId());
        dto.setCodigoCamion(ruta.getCodigoCamion());
        dto.setOrigen(mapUbicacionToDTO(ruta.getOrigen()));
        dto.setDestino(mapUbicacionToDTO(ruta.getDestino()));
        dto.setDistanciaTotal(ruta.getDistanciaTotal());
        dto.setConsumoCombustible(ruta.getConsumoCombustible());
        dto.setCompletada(ruta.isCompletada());
        dto.setCancelada(ruta.isCancelada());
        dto.setHoraInicio(ruta.getHoraInicio());
        dto.setHoraFinEstimada(ruta.getHoraFinEstimada());
        dto.setHoraFinReal(ruta.getHoraFinReal());

        // Mapear secuencia de nodos
        if (ruta.getSecuenciaNodos() != null) {
            dto.setSecuenciaNodos(
                    ruta.getSecuenciaNodos().stream()
                            .map(this::mapUbicacionToDTO)
                            .collect(Collectors.toList())
            );
        }

        // Mapear secuencia de paradas
        if (ruta.getSecuenciaParadas() != null) {
            dto.setSecuenciaParadas(
                    ruta.getSecuenciaParadas().stream()
                            .map(this::mapUbicacionToDTO)
                            .collect(Collectors.toList())
            );
        }

        // Mapear pedidos asignados
        if (ruta.getPedidosAsignados() != null) {
            dto.setPedidosAsignados(
                    ruta.getPedidosAsignados().stream()
                            .map(this::mapPedidoToDTO)
                            .collect(Collectors.toList())
            );
        }

        // Mapear eventos
        if (ruta.getEventos() != null) {
            dto.setEventos(
                    ruta.getEventos().stream()
                            .map(this::mapEventoToDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    /**
     * Convierte una Ubicacion del dominio a CoordenadaDTO
     */
    public RutaVisualizacionDTO.CoordenadaDTO mapUbicacionToDTO(Ubicacion ubicacion) {
        if (ubicacion == null) return null;
        return new RutaVisualizacionDTO.CoordenadaDTO(ubicacion.getX(), ubicacion.getY());
    }

    /**
     * Convierte un Pedido del dominio a PedidoVisualizacionDTO
     */
    public RutaVisualizacionDTO.PedidoVisualizacionDTO mapPedidoToDTO(Pedido pedido) {
        if (pedido == null) return null;

        RutaVisualizacionDTO.PedidoVisualizacionDTO dto = new RutaVisualizacionDTO.PedidoVisualizacionDTO();
        dto.setId(pedido.getId());
        dto.setIdCliente(pedido.getIdCliente());
        dto.setUbicacion(mapUbicacionToDTO(pedido.getUbicacion()));
        dto.setCantidadGLP(pedido.getCantidadGLP());
        dto.setHoraRecepcion(pedido.getHoraRecepcion());
        dto.setHoraLimiteEntrega(pedido.getHoraLimiteEntrega());
        dto.setHoraEntregaProgramada(pedido.getHoraEntregaProgramada());
        dto.setHoraEntregaReal(pedido.getHoraEntregaReal());
        dto.setEntregado(pedido.isEntregado());
        dto.setCamionAsignado(pedido.getCamionAsignado());

        return dto;
    }

    /**
     * Convierte un EventoRuta del dominio a EventoRutaDTO
     */
    public RutaVisualizacionDTO.EventoRutaDTO mapEventoToDTO(EventoRuta evento) {
        if (evento == null) return null;

        RutaVisualizacionDTO.EventoRutaDTO dto = new RutaVisualizacionDTO.EventoRutaDTO();
        dto.setTipo(evento.getTipo().toString());
        dto.setMomento(evento.getMomento());
        dto.setUbicacion(mapUbicacionToDTO(evento.getUbicacion()));
        dto.setDescripcion(evento.getDescripcion());

        return dto;
    }

    /**
     * Convierte un Mapa del dominio a ConfiguracionMapaDTO
     */
    public SimulacionSemanalResponseDTO.ConfiguracionMapaDTO mapMapaToDTO(Mapa mapa) {
        if (mapa == null) return null;

        SimulacionSemanalResponseDTO.ConfiguracionMapaDTO dto = new SimulacionSemanalResponseDTO.ConfiguracionMapaDTO();
        dto.setAncho(mapa.getAncho());
        dto.setAlto(mapa.getAlto());

        // Mapear almacenes
        if (mapa.getAlmacenes() != null) {
            dto.setAlmacenes(
                    mapa.getAlmacenes().stream()
                            .map(this::mapAlmacenToDTO)
                            .collect(Collectors.toList())
            );
        }

        // Mapear bloqueos
        if (mapa.getBloqueos() != null) {
            dto.setBloqueos(
                    mapa.getBloqueos().stream()
                            .map(this::mapBloqueoToDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    /**
     * Convierte un Almacen del dominio a AlmacenDTO
     */
    public SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.AlmacenDTO mapAlmacenToDTO(Almacen almacen) {
        if (almacen == null) return null;

        SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.AlmacenDTO dto = new SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.AlmacenDTO();
        dto.setId(almacen.getId());
        dto.setTipo(almacen.getTipo().toString());
        dto.setUbicacion(mapUbicacionToDTO(almacen.getUbicacion()));
        dto.setCapacidadMaxima(almacen.getCapacidadMaxima());
        dto.setNivelActual(almacen.getNivelActual());

        return dto;
    }

    /**
     * Convierte un Bloqueo del dominio a BloqueoDTO
     */
    public SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.BloqueoDTO mapBloqueoToDTO(Bloqueo bloqueo) {
        if (bloqueo == null) return null;

        SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.BloqueoDTO dto = new SimulacionSemanalResponseDTO.ConfiguracionMapaDTO.BloqueoDTO();
        dto.setId(bloqueo.getId());
        dto.setHoraInicio(bloqueo.getHoraInicio());
        dto.setHoraFin(bloqueo.getHoraFin());

        if (bloqueo.getNodosBloqueados() != null) {
            dto.setNodosBloqueados(
                    bloqueo.getNodosBloqueados().stream()
                            .map(this::mapUbicacionToDTO)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    /**
     * Convierte una lista de Camiones a ResumenCamionesDTO
     */
    public SimulacionSemanalResponseDTO.ResumenCamionesDTO mapCamionesToDTO(List<Camion> camiones) {
        if (camiones == null) return null;

        SimulacionSemanalResponseDTO.ResumenCamionesDTO dto = new SimulacionSemanalResponseDTO.ResumenCamionesDTO();
        dto.setTotalCamiones(camiones.size());

        // Mapear camiones individuales
        dto.setCamiones(
                camiones.stream()
                        .map(this::mapCamionToDTO)
                        .collect(Collectors.toList())
        );

        // Contar por tipos
        Map<String, Integer> conteoTipos = new HashMap<>();
        Map<String, Integer> conteoEstados = new HashMap<>();

        for (Camion camion : camiones) {
            String tipo = camion.getTipo().toString();
            String estado = camion.getEstado().toString();

            conteoTipos.put(tipo, conteoTipos.getOrDefault(tipo, 0) + 1);
            conteoEstados.put(estado, conteoEstados.getOrDefault(estado, 0) + 1);
        }

        dto.setConteoTipos(conteoTipos);
        dto.setConteoEstados(conteoEstados);

        return dto;
    }

    /**
     * Convierte un Camion del dominio a CamionDTO
     */
    public SimulacionSemanalResponseDTO.ResumenCamionesDTO.CamionDTO mapCamionToDTO(Camion camion) {
        if (camion == null) return null;

        SimulacionSemanalResponseDTO.ResumenCamionesDTO.CamionDTO dto = new SimulacionSemanalResponseDTO.ResumenCamionesDTO.CamionDTO();
        dto.setCodigo(camion.getCodigo());
        dto.setTipo(camion.getTipo().toString());
        dto.setEstado(camion.getEstado().toString());
        dto.setUbicacionActual(mapUbicacionToDTO(camion.getUbicacionActual()));
        dto.setCapacidadTanqueGLP(camion.getCapacidadTanqueGLP());
        dto.setNivelGLPActual(camion.getNivelGLPActual());
        dto.setNivelCombustibleActual(camion.getNivelCombustibleActual());
        dto.setEnMantenimiento(camion.isEnMantenimiento());
        dto.setAveriado(camion.isAveriado());

        return dto;
    }

    /**
     * Estructura las métricas de rendimiento
     */
    public SimulacionSemanalResponseDTO.MetricasRendimientoDTO mapMetricasToDTO(Map<String, Object> estadisticas) {
        SimulacionSemanalResponseDTO.MetricasRendimientoDTO dto = new SimulacionSemanalResponseDTO.MetricasRendimientoDTO();

        // Extraer métricas de fitness
        if (estadisticas.containsKey("fitness")) {
            dto.setFitness((Map<String, Object>) estadisticas.get("fitness"));
        }

        // Extraer métricas de tiempo
        if (estadisticas.containsKey("tiempoEjecucion")) {
            dto.setTiempoEjecucion((Map<String, Object>) estadisticas.get("tiempoEjecucion"));
        }

        // Eficiencia de tiempo
        if (estadisticas.containsKey("eficienciaTiempo")) {
            dto.setEficienciaTiempo((Double) estadisticas.get("eficienciaTiempo"));
        }

        // Métricas diarias (si existen)
        Map<String, Object> metricasDiarias = new HashMap<>();
        if (estadisticas.containsKey("resultadosPorDia")) {
            Map<Integer, Map<String, Object>> resultadosPorDia = (Map<Integer, Map<String, Object>>) estadisticas.get("resultadosPorDia");

            Map<String, Double> distanciaPorDia = new HashMap<>();
            Map<String, Integer> entregasPorDia = new HashMap<>();
            Map<String, Double> fitnessPorDia = new HashMap<>();

            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                double distanciaDia = (double) datosDia.getOrDefault("distanciaTotal", 0.0);
                int entregasDia = (int) datosDia.getOrDefault("pedidosEntregados", 0);
                double fitnessDia = datosDia.containsKey("fitness") ? (double) datosDia.get("fitness") : 0.0;

                distanciaPorDia.put("Día " + dia, distanciaDia);
                entregasPorDia.put("Día " + dia, entregasDia);
                fitnessPorDia.put("Día " + dia, fitnessDia);
            }

            metricasDiarias.put("distanciaPorDia", distanciaPorDia);
            metricasDiarias.put("entregasPorDia", entregasPorDia);
            metricasDiarias.put("fitnessPorDia", fitnessPorDia);
        }

        dto.setMetricasDiarias(metricasDiarias);
        return dto;
    }

    /**
     * Mapea los resultados por día a DTOs estructurados
     */
    public Map<Integer, SimulacionSemanalResponseDTO.ResultadoDiaDTO> mapResultadosPorDiaToDTO(
            Map<Integer, Map<String, Object>> resultadosPorDia, LocalDateTime fechaInicio) {

        Map<Integer, SimulacionSemanalResponseDTO.ResultadoDiaDTO> dtoMap = new HashMap<>();

        if (resultadosPorDia != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                SimulacionSemanalResponseDTO.ResultadoDiaDTO diaDTO = new SimulacionSemanalResponseDTO.ResultadoDiaDTO();
                diaDTO.setDia(dia);
                diaDTO.setFecha(fechaInicio.plusDays(dia - 1));
                diaDTO.setPedidosAsignados((int) datosDia.getOrDefault("pedidosAsignados", 0));
                diaDTO.setPedidosEntregados((int) datosDia.getOrDefault("pedidosEntregados", 0));
                diaDTO.setPedidosRetrasados((int) datosDia.getOrDefault("pedidosRetrasados", 0));
                diaDTO.setDistanciaTotal((double) datosDia.getOrDefault("distanciaTotal", 0.0));
                diaDTO.setConsumoCombustible((double) datosDia.getOrDefault("consumoCombustible", 0.0));
                diaDTO.setAveriasOcurridas((int) datosDia.getOrDefault("averiasOcurridas", 0));
                diaDTO.setFitness(datosDia.containsKey("fitness") ? (double) datosDia.get("fitness") : 0.0);
                diaDTO.setTiempoEjecucionMs((long) datosDia.getOrDefault("tiempoEjecucionMs", 0L));

                // Mapear rutas del día
                if (datosDia.containsKey("rutas")) {
                    List<Ruta> rutas = (List<Ruta>) datosDia.get("rutas");
                    diaDTO.setRutas(
                            rutas.stream()
                                    .map(this::mapRutaToDTO)
                                    .collect(Collectors.toList())
                    );
                }

                // Mapear camiones en mantenimiento
                if (datosDia.containsKey("camionesEnMantenimiento")) {
                    diaDTO.setCamionesEnMantenimiento((List<String>) datosDia.get("camionesEnMantenimiento"));
                }

                dtoMap.put(dia, diaDTO);
            }
        }

        return dtoMap;
    }

    /**
     * Estructura las rutas por día para visualización rápida
     */
    public Map<String, List<RutaVisualizacionDTO>> mapRutasPorDiaToDTO(Map<Integer, Map<String, Object>> resultadosPorDia) {
        Map<String, List<RutaVisualizacionDTO>> rutasPorDia = new HashMap<>();

        if (resultadosPorDia != null) {
            for (Map.Entry<Integer, Map<String, Object>> entry : resultadosPorDia.entrySet()) {
                int dia = entry.getKey();
                Map<String, Object> datosDia = entry.getValue();

                if (datosDia.containsKey("rutas")) {
                    List<Ruta> rutas = (List<Ruta>) datosDia.get("rutas");
                    List<RutaVisualizacionDTO> rutasDTO = rutas.stream()
                            .map(this::mapRutaToDTO)
                            .collect(Collectors.toList());

                    rutasPorDia.put("dia_" + dia, rutasDTO);
                }
            }
        }

        return rutasPorDia;
    }
}