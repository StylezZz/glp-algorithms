package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final DataRepository dataRepository;

    private static final Pattern FECHA_HORA_PATTERN = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");
    private static final Pattern FECHA_HORA_RANGO_PATTERN = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m-(\\d+)d(\\d+)h(\\d+)m");

    public List<Pedido> cargarPedidosPorDia(InputStream inputStream,LocalDateTime fechaReferencia){
        List<Pedido> todosPedidos = cargarPedidos(inputStream);

        return todosPedidos.stream().filter(pedido -> esMismoDia(pedido.getHoraRecepcion(),fechaReferencia)).collect(Collectors.toList());
    }

    public List<Pedido> cargarPedidosSemExp(InputStream inputStream,LocalDateTime fechaInicio, LocalDateTime fechaFin){
        List<Pedido> todoPedidos = cargarPedidos(inputStream);

        return todoPedidos.stream().filter(pedido -> esDentroDelPeriodo(pedido.getHoraRecepcion(), fechaInicio, fechaFin)).collect(Collectors.toList());
    }

    // Nuevo método - Cargar pedidos por semana
    public List<Pedido> cargarPedidosPorSemana(InputStream inputStream, LocalDateTime fechaReferencia) {
        List<Pedido> todosPedidos = cargarPedidos(inputStream);
        
        // Calcular el inicio y fin de la semana (lunes a domingo)
        LocalDateTime inicioDeSemana = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime finDeSemana = inicioDeSemana.plusDays(7).minusNanos(1);
        
        log.info("Filtrando pedidos de la semana: {} a {}", inicioDeSemana, finDeSemana);
        
        return todosPedidos.stream()
                .filter(pedido -> esDentroDelPeriodo(pedido.getHoraRecepcion(), inicioDeSemana, finDeSemana))
                .collect(Collectors.toList());
    }

    // Método auxiliar - Verificar si una fecha está dentro de un período
    private boolean esDentroDelPeriodo(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        return (fecha.isEqual(inicio) || fecha.isAfter(inicio)) && 
               (fecha.isEqual(fin) || fecha.isBefore(fin));
    }

    private boolean esMismoDia(LocalDateTime fecha1, LocalDateTime fecha2) {
        return fecha1.getYear() == fecha2.getYear() &&
                fecha1.getDayOfYear() == fecha2.getDayOfYear();
    }

    @Override
    public List<Pedido> cargarPedidos(InputStream inputStream) {
        List<Pedido> pedidos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Pedido pedido = parsearLineaPedido(line);
                    if (pedido != null) {
                        pedidos.add(pedido);
                    }
                } catch (Exception e) {
                    log.error("Error al parsear línea de pedido: {}", line, e);
                }
            }
        } catch (IOException e) {
            log.error("Error al leer archivo de pedidos", e);
            throw new RuntimeException("Error al leer archivo de pedidos", e);
        }

        // Guardar pedidos en el repositorio
        if (!pedidos.isEmpty()) {
            dataRepository.guardarPedidos(pedidos);
        }

        log.info("Cargados {} pedidos del archivo", pedidos.size());
        return pedidos;
    }

    @Override
    public List<Bloqueo> cargarBloqueos(InputStream inputStream) {
        List<Bloqueo> bloqueos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Bloqueo bloqueo = parsearLineaBloqueo(line);
                    if (bloqueo != null) {
                        bloqueos.add(bloqueo);
                    }
                } catch (Exception e) {
                    log.error("Error al parsear línea de bloqueo: {}", line, e);
                }
            }
        } catch (IOException e) {
            log.error("Error al leer archivo de bloqueos", e);
            throw new RuntimeException("Error al leer archivo de bloqueos", e);
        }

        // Guardar bloqueos en el repositorio
        if (!bloqueos.isEmpty()) {
            dataRepository.guardarBloqueos(bloqueos);
        }

        log.info("Cargados {} bloqueos del archivo", bloqueos.size());
        return bloqueos;
    }

    /**
     * Parsea una línea del archivo de pedidos
     * Formato: ##d##h##m:posX,posY,c-idCliente,m3,hLímite
     * Ejemplo: 01d00h06m:68,34,c-36,13m3,11h
     */
    private Pedido parsearLineaPedido(String linea) {
        String[] partes = linea.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de línea inválido: " + linea);
        }

        String momentoStr = partes[0];
        String[] datosPedido = partes[1].split(",");

        if (datosPedido.length != 5) {
            throw new IllegalArgumentException("Datos de pedido inválidos: " + partes[1]);
        }

        // Obtener ubicación del pedido
        int posX = Integer.parseInt(datosPedido[0]);
        int posY = Integer.parseInt(datosPedido[1]);
        Ubicacion ubicacion = new Ubicacion(posX, posY);

        // Obtener ID del cliente (formato c-###)
        String idCliente = datosPedido[2];

        // Obtener cantidad de GLP (formato ##m3)
        String cantidadStr = datosPedido[3];
        double cantidadGLP = Double.parseDouble(cantidadStr.replace("m3", ""));

        // Obtener horas límite (formato ##h)
        String horasLimiteStr = datosPedido[4];
        int horasLimite = Integer.parseInt(horasLimiteStr.replace("h", ""));

        // Crear el pedido
        return new Pedido(idCliente, ubicacion, cantidadGLP, momentoStr, horasLimite);
    }

    /**
     * Parsea una línea del archivo de bloqueos
     * Formato: ##d##h##m-##d##h##m:x1,y1,x2,y2,...
     * Ejemplo: 01d00h28m-01d20h48m:05,20,05,35
     */
    private Bloqueo parsearLineaBloqueo(String linea) {
        String[] partes = linea.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de línea inválido: " + linea);
        }

        // Parsear rango de fechas/horas
        Matcher matcher = FECHA_HORA_RANGO_PATTERN.matcher(partes[0]);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Formato de rango de tiempo inválido: " + partes[0]);
        }

        int diaInicio = Integer.parseInt(matcher.group(1));
        int horaInicio = Integer.parseInt(matcher.group(2));
        int minutoInicio = Integer.parseInt(matcher.group(3));

        int diaFin = Integer.parseInt(matcher.group(4));
        int horaFin = Integer.parseInt(matcher.group(5));
        int minutoFin = Integer.parseInt(matcher.group(6));

        // Año y mes actuales
        LocalDateTime ahora = LocalDateTime.now();
        int year = ahora.getYear();
        int mes = ahora.getMonthValue();

        LocalDateTime horaInicioVar = LocalDateTime.of(year, mes, diaInicio, horaInicio, minutoInicio);
        LocalDateTime horaFinVar = LocalDateTime.of(year, mes, diaFin, horaFin, minutoFin);

        // Parsear coordenadas
        String[] coordenadasStr = partes[1].split(",");
        if (coordenadasStr.length % 2 != 0) {
            throw new IllegalArgumentException("Número impar de coordenadas: " + partes[1]);
        }

        List<Ubicacion> nodosBloqueados = new ArrayList<>();
        for (int i = 0; i < coordenadasStr.length; i += 2) {
            int x = Integer.parseInt(coordenadasStr[i]);
            int y = Integer.parseInt(coordenadasStr[i + 1]);
            nodosBloqueados.add(new Ubicacion(x, y));
        }

        return new Bloqueo(horaInicioVar, horaFinVar, nodosBloqueados);
    }
}