// Crear un nuevo servicio: AnalisisPedidosService.java

package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;
import com.glp.glpDP1.domain.Ubicacion;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalisisPedidosService {

    /**
     * Analiza los pedidos no asignados para identificar patrones
     */
    public Map<String, Object> analizarPedidosNoAsignados(
            List<Pedido> todosPedidos,
            List<Ruta> rutas) {

        // Obtener IDs de pedidos asignados
        Set<String> idsPedidosAsignados = rutas.stream()
                .flatMap(r -> r.getPedidosAsignados().stream())
                .map(Pedido::getId)
                .collect(Collectors.toSet());

        // Filtrar pedidos no asignados
        List<Pedido> pedidosNoAsignados = todosPedidos.stream()
                .filter(p -> !idsPedidosAsignados.contains(p.getId()))
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalPedidos", todosPedidos.size());
        resultado.put("pedidosAsignados", idsPedidosAsignados.size());
        resultado.put("pedidosNoAsignados", pedidosNoAsignados.size());

        if (pedidosNoAsignados.isEmpty()) {
            resultado.put("mensaje", "Todos los pedidos fueron asignados correctamente.");
            return resultado;
        }

        // Análisis por tamaño de pedido
        Map<String, Long> distribucionPorTamaño = new HashMap<>();
        distribucionPorTamaño.put("pequeños (0-5m³)",
                pedidosNoAsignados.stream().filter(p -> p.getCantidadGLP() <= 5).count());
        distribucionPorTamaño.put("medianos (5-15m³)",
                pedidosNoAsignados.stream().filter(p -> p.getCantidadGLP() > 5 && p.getCantidadGLP() <= 15).count());
        distribucionPorTamaño.put("grandes (15-25m³)",
                pedidosNoAsignados.stream().filter(p -> p.getCantidadGLP() > 15 && p.getCantidadGLP() <= 25).count());
        distribucionPorTamaño.put("muy grandes (>25m³)",
                pedidosNoAsignados.stream().filter(p -> p.getCantidadGLP() > 25).count());

        resultado.put("distribucionPorTamaño", distribucionPorTamaño);

        // Análisis por urgencia
        Map<String, Long> distribucionPorUrgencia = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();

        distribucionPorUrgencia.put("urgentes (<4h)",
                pedidosNoAsignados.stream()
                        .filter(p -> ChronoUnit.HOURS.between(p.getHoraRecepcion(), p.getHoraLimiteEntrega()) <= 4)
                        .count());
        distribucionPorUrgencia.put("normales (4-12h)",
                pedidosNoAsignados.stream()
                        .filter(p -> {
                            long horas = ChronoUnit.HOURS.between(p.getHoraRecepcion(), p.getHoraLimiteEntrega());
                            return horas > 4 && horas <= 12;
                        })
                        .count());
        distribucionPorUrgencia.put("no urgentes (>12h)",
                pedidosNoAsignados.stream()
                        .filter(p -> ChronoUnit.HOURS.between(p.getHoraRecepcion(), p.getHoraLimiteEntrega()) > 12)
                        .count());

        resultado.put("distribucionPorUrgencia", distribucionPorUrgencia);

        // Análisis por distancia desde el almacén central (asumiendo coordenadas 12,8)
        Ubicacion almacenCentral = new Ubicacion(12, 8);

        int distanciaPromedio = (int) pedidosNoAsignados.stream()
                .mapToInt(p -> p.getUbicacion().distanciaA(almacenCentral))
                .average()
                .orElse(0);

        int distanciaMaxima = pedidosNoAsignados.stream()
                .mapToInt(p -> p.getUbicacion().distanciaA(almacenCentral))
                .max()
                .orElse(0);

        resultado.put("distanciaPromedio", distanciaPromedio);
        resultado.put("distanciaMaxima", distanciaMaxima);

        // Pedidos más problemáticos (grandes y lejos del almacén central)
        List<Map<String, Object>> pedidosProblemáticos = pedidosNoAsignados.stream()
                .filter(p -> p.getCantidadGLP() > 15 && p.getUbicacion().distanciaA(almacenCentral) > 30)
                .sorted(Comparator.comparingDouble(Pedido::getCantidadGLP).reversed())
                .limit(5)
                .map(p -> {
                    Map<String, Object> pedido = new HashMap<>();
                    pedido.put("id", p.getId());
                    pedido.put("idCliente", p.getIdCliente());
                    pedido.put("cantidadGLP", p.getCantidadGLP());
                    pedido.put("ubicacion", p.getUbicacion().toString());
                    pedido.put("distanciaAlmacen", p.getUbicacion().distanciaA(almacenCentral));
                    pedido.put("horasLimite", ChronoUnit.HOURS.between(p.getHoraRecepcion(), p.getHoraLimiteEntrega()));
                    return pedido;
                })
                .collect(Collectors.toList());

        resultado.put("pedidosProblemáticos", pedidosProblemáticos);

        // Recomendaciones basadas en el análisis
        List<String> recomendaciones = new ArrayList<>();

        if (distribucionPorTamaño.get("muy grandes (>25m³)") > 0) {
            recomendaciones.add("Revisar la estrategia de división de pedidos muy grandes (>25m³)");
        }

        if (distribucionPorUrgencia.get("urgentes (<4h)") > pedidosNoAsignados.size() * 0.3) {
            recomendaciones.add("Ajustar la priorización de pedidos urgentes (menos de 4 horas)");
        }

        if (distanciaPromedio > 40) {
            recomendaciones.add("Considerar ubicar camiones más cerca de zonas distantes");
        }

        resultado.put("recomendaciones", recomendaciones);

        return resultado;
    }

    /**
     * Genera un informe detallado de los pedidos no asignados
     */
    public String generarInformeNoAsignados(List<Pedido> todosPedidos, List<Ruta> rutas) {
        Map<String, Object> analisis = analizarPedidosNoAsignados(todosPedidos, rutas);

        StringBuilder sb = new StringBuilder();
        sb.append("# INFORME DE PEDIDOS NO ASIGNADOS\n\n");

        sb.append("## Resumen\n");
        sb.append("- Total de pedidos: ").append(analisis.get("totalPedidos")).append("\n");
        sb.append("- Pedidos asignados: ").append(analisis.get("pedidosAsignados")).append("\n");
        sb.append("- Pedidos no asignados: ").append(analisis.get("pedidosNoAsignados")).append("\n\n");

        if ((int)analisis.get("pedidosNoAsignados") == 0) {
            sb.append(analisis.get("mensaje")).append("\n");
            return sb.toString();
        }

        sb.append("## Distribución por tamaño\n");
        Map<String, Long> porTamaño = (Map<String, Long>) analisis.get("distribucionPorTamaño");
        for (Map.Entry<String, Long> entrada : porTamaño.entrySet()) {
            sb.append("- ").append(entrada.getKey()).append(": ").append(entrada.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("## Distribución por urgencia\n");
        Map<String, Long> porUrgencia = (Map<String, Long>) analisis.get("distribucionPorUrgencia");
        for (Map.Entry<String, Long> entrada : porUrgencia.entrySet()) {
            sb.append("- ").append(entrada.getKey()).append(": ").append(entrada.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("## Distancia desde almacén central\n");
        sb.append("- Distancia promedio: ").append(analisis.get("distanciaPromedio")).append(" km\n");
        sb.append("- Distancia máxima: ").append(analisis.get("distanciaMaxima")).append(" km\n\n");

        sb.append("## Pedidos más problemáticos\n");
        List<Map<String, Object>> problemáticos = (List<Map<String, Object>>) analisis.get("pedidosProblemáticos");
        for (Map<String, Object> pedido : problemáticos) {
            sb.append("- Cliente: ").append(pedido.get("idCliente"))
                    .append(", GLP: ").append(pedido.get("cantidadGLP")).append("m³")
                    .append(", Ubicación: ").append(pedido.get("ubicacion"))
                    .append(", Distancia: ").append(pedido.get("distanciaAlmacen")).append("km")
                    .append(", Horas límite: ").append(pedido.get("horasLimite")).append("h\n");
        }
        sb.append("\n");

        sb.append("## Recomendaciones\n");
        List<String> recomendaciones = (List<String>) analisis.get("recomendaciones");
        for (String recomendacion : recomendaciones) {
            sb.append("- ").append(recomendacion).append("\n");
        }

        return sb.toString();
    }
}
