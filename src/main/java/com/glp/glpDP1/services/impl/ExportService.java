package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ruta;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ExportService {
    public void exportarResultadosCSV(AlgoritmoResultResponse resultado, String rutaArchivo) throws IOException {
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            // Escribir cabecera de resumen
            writer.write("ID Ejecución,Tipo Algoritmo,Fitness,Distancia Total,Consumo Combustible,Pedidos Entregados,Pedidos Totales,Tiempo Ejecución\n");
            writer.write(String.format("%s,%.2f,%.2f,%.2f,%d,%d,%d ms\n\n",
                    resultado.getId(),
                    resultado.getFitness(),
                    resultado.getDistanciaTotal(),
                    resultado.getConsumoCombustible(),
                    resultado.getPedidosEntregados(),
                    resultado.getPedidosTotales(),
                    resultado.getTiempoEjecucion().toMillis()));

            // Escribir cabecera de rutas
            writer.write("Código Camión,Pedidos Asignados,Distancia (km),Consumo (gal),Origen,Destino\n");

            // Escribir información de cada ruta
            for (Ruta ruta : resultado.getRutas()) {
                writer.write(String.format("%s,%d,%.2f,%.2f,%s,%s\n",
                        ruta.getCodigoCamion(),
                        ruta.getPedidosAsignados().size(),
                        ruta.getDistanciaTotal(),
                        ruta.getConsumoCombustible(),
                        ruta.getOrigen(),
                        ruta.getDestino()));
            }

            // Escribir detalles de pedidos
            writer.write("\nDetalles de Pedidos\n");
            writer.write("ID Pedido,Cliente,Ubicación,Cantidad GLP,Hora Recepción,Hora Límite,Camión Asignado,Entregado\n");

            // Obtener todos los pedidos de todas las rutas
            List<Pedido> todosPedidos = resultado.getRutas().stream()
                    .flatMap(r -> r.getPedidosAsignados().stream())
                    .collect(Collectors.toList());

            for (Pedido pedido : todosPedidos) {
                writer.write(String.format("%s,%s,%s,%.2f,%s,%s,%s,%s\n",
                        pedido.getId(),
                        pedido.getIdCliente(),
                        pedido.getUbicacion(),
                        pedido.getCantidadGLP(),
                        pedido.getHoraRecepcion(),
                        pedido.getHoraLimiteEntrega(),
                        pedido.getCamionAsignado(),
                        pedido.isEntregado() ? "Sí" : "No"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
