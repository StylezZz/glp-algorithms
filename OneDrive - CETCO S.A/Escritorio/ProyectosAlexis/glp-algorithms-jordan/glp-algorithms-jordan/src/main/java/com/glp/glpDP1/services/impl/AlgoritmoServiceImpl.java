package com.glp.glpDP1.services.impl;

import com.glp.glpDP1.algorithm.AlgoritmoGenetico;
import com.glp.glpDP1.algorithm.AlgoritmoPSO;
import com.glp.glpDP1.api.dto.request.AlgoritmoRequest;
import com.glp.glpDP1.api.dto.request.AlgoritmoSimpleRequest;
import com.glp.glpDP1.api.dto.response.AlgoritmoResultResponse;
import com.glp.glpDP1.api.dto.response.AlgoritmoStatusResponse;
import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EscenarioSimulacion;
import com.glp.glpDP1.repository.DataRepository;
import com.glp.glpDP1.services.AlgoritmoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlgoritmoServiceImpl implements AlgoritmoService {

    private final DataRepository dataRepository;
    private final MonitoreoService monitoreoService;
    // Almacena las ejecuciones en curso
    private final Map<String, Future<?>> tareas = new ConcurrentHashMap<>();

    // Almacena el estado de cada ejecución
    private final Map<String, AlgoritmoStatusResponse> estados = new ConcurrentHashMap<>();

    // Almacena los resultados de las ejecuciones completadas
    private final Map<String, AlgoritmoResultResponse> resultados = new ConcurrentHashMap<>();

    // Executor para ejecutar los algoritmos de forma asíncrona
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    @Override
    public String iniciarAlgoritmo(AlgoritmoRequest request) {
        // Este método se mantiene para compatibilidad con código existente

        // Validar petición
        if (request.getCamiones() == null || request.getCamiones().isEmpty()) {
            throw new IllegalArgumentException("No se proporcionaron camiones");
        }
        if (request.getPedidos() == null || request.getPedidos().isEmpty()) {
            throw new IllegalArgumentException("No se proporcionaron pedidos");
        }
        if (request.getMapa() == null) {
            throw new IllegalArgumentException("No se proporcionó el mapa");
        }
        if (request.getMomentoActual() == null) {
            throw new IllegalArgumentException("No se proporcionó el momento actual");
        }

        return iniciarEjecucionAlgoritmo(
                request.getTipoAlgoritmo(),
                request.getCamiones(),
                request.getPedidos(),
                request.getMapa(),
                request.getMomentoActual(),
                request.getEscenario(),
                request.getTamañoPoblacion(),
                request.getNumGeneraciones(),
                request.getTasaMutacion(),
                request.getTasaCruce(),
                request.getElitismo(),
                request.getNumParticulas(),
                request.getNumIteraciones(),
                request.getW(),
                request.getC1(),
                request.getC2()
        );
    }

    @Override
    public String iniciarAlgoritmo(AlgoritmoSimpleRequest request) {
        // Obtener datos del repositorio
        List<Camion> camiones = dataRepository.obtenerCamiones();
        List<Pedido> pedidos = dataRepository.obtenerPedidos();
        Mapa mapa = dataRepository.obtenerMapa();

        // Validar datos
        if (camiones.isEmpty()) {
            throw new IllegalArgumentException("No hay camiones disponibles en el sistema");
        }
        if (pedidos.isEmpty()) {
            throw new IllegalArgumentException("No hay pedidos pendientes en el sistema");
        }
        if (mapa == null) {
            throw new IllegalArgumentException("No hay mapa configurado en el sistema");
        }

        // Si no se especificó momento actual, usar el actual
        LocalDateTime momentoActual = request.getMomentoActual();
        if (momentoActual == null) {
            momentoActual = LocalDateTime.now();
        }

        // Convertir escenario
        EscenarioSimulacion escenario = null;
        if (request.getEscenario() != null) {
            try {
                escenario = EscenarioSimulacion.valueOf(request.getEscenario());
            } catch (IllegalArgumentException e) {
                log.warn("Escenario no válido: {}, usando DIA_A_DIA por defecto", request.getEscenario());
            }
        }
        if (escenario == null) {
            escenario = EscenarioSimulacion.DIA_A_DIA;
        }

        return iniciarEjecucionAlgoritmo(
                request.getTipoAlgoritmo(),
                camiones,
                pedidos,
                mapa,
                momentoActual,
                escenario,
                request.getTamañoPoblacion(),
                request.getNumGeneraciones(),
                request.getTasaMutacion(),
                request.getTasaCruce(),
                request.getElitismo(),
                request.getNumParticulas(),
                request.getNumIteraciones(),
                request.getW(),
                request.getC1(),
                request.getC2()
        );
    }

    /**
     * Método interno para iniciar la ejecución del algoritmo
     */
    private String iniciarEjecucionAlgoritmo(
            String tipoAlgoritmo,
            List<Camion> camiones,
            List<Pedido> pedidos,
            Mapa mapa,
            LocalDateTime momentoActual,
            EscenarioSimulacion escenario,
            Integer tamañoPoblacion,
            Integer numGeneraciones,
            Double tasaMutacion,
            Double tasaCruce,
            Integer elitismo,
            Integer numParticulas,
            Integer numIteraciones,
            Double w,
            Double c1,
            Double c2
    ) {
        // Generar un ID único para esta ejecución
        String id = UUID.randomUUID().toString();

        // Crear objeto de estado inicial
        AlgoritmoStatusResponse estado = new AlgoritmoStatusResponse(
                id,
                AlgoritmoStatusResponse.EstadoAlgoritmo.PENDIENTE,
                0.0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
        estados.put(id, estado);

        // Ejecutar el algoritmo de forma asíncrona
        Future<?> tarea = executor.submit(() -> {
            try {
                // Actualizar estado a EN_EJECUCION
                estado.setEstado(AlgoritmoStatusResponse.EstadoAlgoritmo.EN_EJECUCION);

                // Medir tiempo de inicio
                LocalDateTime horaInicio = LocalDateTime.now();

                // Ejecutar el algoritmo correspondiente
                List<Ruta> rutas;
                double fitness;

                if ("GENETICO".equalsIgnoreCase(tipoAlgoritmo)) {
                    // Algoritmo Genético
                    AlgoritmoGenetico algoritmo;

                    // Configurar parámetros si se proporcionaron
                    if (tamañoPoblacion != null &&
                            numGeneraciones != null &&
                            tasaMutacion != null &&
                            tasaCruce != null &&
                            elitismo != null) {

                        algoritmo = new AlgoritmoGenetico(
                                tamañoPoblacion,
                                numGeneraciones,
                                tasaMutacion,
                                tasaCruce,
                                elitismo
                        );
                    } else {
                        // Usar parámetros por defecto
                        algoritmo = new AlgoritmoGenetico();
                    }
                    algoritmo.setMonitoreoService(monitoreoService);
                    // Ejecutar optimización
                    rutas = algoritmo.optimizarRutas(
                            camiones,
                            pedidos,
                            mapa,
                            momentoActual
                    );

                    SimuladorEntregas simulador = new SimuladorEntregas();
                    rutas = simulador.simularEntregas(rutas, momentoActual);

                    fitness = algoritmo.getMejorFitness();

                    // Actualizar progreso durante la ejecución
                    for (int i = 0; i < 100 && !Thread.currentThread().isInterrupted(); i++) {
                        estado.setProgreso(i);
                        estado.setHoraUltimaActualizacion(LocalDateTime.now());
                        estado.setMejorFitness(algoritmo.getMejorFitness());
                        Thread.sleep(50); // Simular tiempo de ejecución
                    }

                } else if ("PSO".equalsIgnoreCase(tipoAlgoritmo)) {
                    // Algoritmo PSO
                    AlgoritmoPSO algoritmo;

                    // Configurar parámetros si se proporcionaron
                    if (numParticulas != null &&
                            numIteraciones != null &&
                            w != null &&
                            c1 != null &&
                            c2 != null) {

                        algoritmo = new AlgoritmoPSO(
                                numParticulas,
                                numIteraciones,
                                w,
                                c1,
                                c2
                        );
                    } else {
                        // Usar parámetros por defecto
                        algoritmo = new AlgoritmoPSO();
                    }

                    // Ejecutar optimización
                    rutas = algoritmo.optimizarRutas(
                            camiones,
                            pedidos,
                            mapa,
                            momentoActual
                    );

                    fitness = algoritmo.getMejorFitness();
                } else {
                    throw new IllegalArgumentException("Tipo de algoritmo no soportado: " + tipoAlgoritmo);
                }

                // Medir tiempo de fin
                LocalDateTime horaFin = LocalDateTime.now();
                Duration tiempoEjecucion = Duration.between(horaInicio, horaFin);

                // Calcular métricas adicionales
                double distanciaTotal = calcularDistanciaTotal(rutas);
                double consumoCombustible = calcularConsumoCombustible(rutas);
                int pedidosEntregados = calcularPedidosEntregados(rutas);
                double maxCapacidadDisponible = camiones.stream().mapToDouble(Camion::getCapacidadTanqueGLP).max().orElse(0.0);
                List<Map<String,Object>> pedidosNoAsignados = new ArrayList<>();
                for(Pedido pedido: pedidos){
                    boolean asignado = false;
                    for(Ruta ruta:rutas){
                        if(ruta.getPedidosAsignados().contains(pedido)){
                            asignado = true;
                            break;
                        }
                    }
                    if(!asignado){
                        Map<String,Object> pedidosNoAsignado = new HashMap<>();
                        pedidosNoAsignado.put("idPedido",pedido.getId());
                        pedidosNoAsignado.put("cantidadGLP",pedido.getCantidadGLP());
                        pedidosNoAsignado.put("horaRecepcion",pedido.getHoraRecepcion());

                        String razon;
                        if(pedido.getCantidadGLP() > maxCapacidadDisponible){
                            razon = "Excede capacidad maxima de camiones disponibles";
                        }else if(pedido.getHoraLimiteEntrega().isBefore(momentoActual)){
                            razon = "Fuera de ventana de tiempo";
                        }else{
                            razon = "No se pudo optimizar en las rutas disponibles";
                        }
                        pedidosNoAsignado.put("razon", razon);
                        pedidosNoAsignados.add(pedidosNoAsignado);
                    }
                }

                // Crear objeto de resultado
                Map<String, Object> metricas = new HashMap<>();
                metricas.put("tiempoEjecucionMs", tiempoEjecucion.toMillis());
                metricas.put("escenarioSimulacion", escenario);

                AlgoritmoResultResponse resultado = new AlgoritmoResultResponse(
                        id,
                        rutas,
                        fitness,
                        distanciaTotal,
                        consumoCombustible,
                        pedidosEntregados,
                        pedidos.size(),
                        metricas,
                        tipoAlgoritmo,
                        horaInicio,
                        horaFin,
                        tiempoEjecucion
                );

                // Guardar resultado
                resultados.put(id, resultado);

                // Actualizar estado a COMPLETADO
                estado.setEstado(AlgoritmoStatusResponse.EstadoAlgoritmo.COMPLETADO);
                estado.setProgreso(100);
                estado.setHoraUltimaActualizacion(horaFin);
                estado.setMejorFitness(fitness);

                log.info("Algoritmo {} completado para ID {}, fitness: {}",
                        tipoAlgoritmo, id, fitness);

            } catch (Exception e) {
                // Actualizar estado a FALLIDO
                estado.setEstado(AlgoritmoStatusResponse.EstadoAlgoritmo.FALLIDO);
                estado.setHoraUltimaActualizacion(LocalDateTime.now());
                log.error("Error en ejecución de algoritmo {}: {}", id, e.getMessage(), e);
            }
        });

        // Almacenar la tarea
        tareas.put(id, tarea);

        return id;
    }

    @Override
    public AlgoritmoStatusResponse consultarEstado(String id) {
        AlgoritmoStatusResponse estado = estados.get(id);
        if (estado == null) {
            throw new NoSuchElementException("No se encontró la ejecución con ID: " + id);
        }
        return estado;
    }

    @Override
    public AlgoritmoResultResponse obtenerResultados(String id) {
        AlgoritmoResultResponse resultado = resultados.get(id);
        if (resultado == null) {
            AlgoritmoStatusResponse estado = estados.get(id);
            if (estado == null) {
                throw new NoSuchElementException("No se encontró la ejecución con ID: " + id);
            }

            if (estado.getEstado() == AlgoritmoStatusResponse.EstadoAlgoritmo.EN_EJECUCION ||
                    estado.getEstado() == AlgoritmoStatusResponse.EstadoAlgoritmo.PENDIENTE) {
                throw new IllegalStateException("La ejecución aún no ha terminado");
            } else if (estado.getEstado() == AlgoritmoStatusResponse.EstadoAlgoritmo.FALLIDO) {
                throw new IllegalStateException("La ejecución falló y no generó resultados");
            }

            throw new NoSuchElementException("No se encontraron resultados para la ejecución con ID: " + id);
        }
        return resultado;
    }

    @Override
    public boolean cancelarEjecucion(String id) {
        Future<?> tarea = tareas.get(id);
        if (tarea == null) {
            return false;
        }

        boolean cancelled = tarea.cancel(true);
        if (cancelled) {
            AlgoritmoStatusResponse estado = estados.get(id);
            if (estado != null) {
                estado.setEstado(AlgoritmoStatusResponse.EstadoAlgoritmo.FALLIDO);
                estado.setHoraUltimaActualizacion(LocalDateTime.now());
            }
        }
        return cancelled;
    }

    // Métodos auxiliares para cálculo de métricas

    private double calcularDistanciaTotal(List<Ruta> rutas) {
        return rutas.stream().mapToDouble(Ruta::getDistanciaTotal).sum();
    }

    private double calcularConsumoCombustible(List<Ruta> rutas) {
        double totalConsumo = 0.0;
        for(Ruta ruta:rutas){
            totalConsumo += ruta.getConsumoCombustible();
            if(ruta.getConsumoCombustible()<=0 && ruta.getDistanciaTotal()>0){
                for(Camion camion: dataRepository.obtenerCamiones()){
                    if(camion.getCodigo().equals(ruta.getCodigoCamion())){
                        double consumo = ruta.calcularConsumoCombustible(camion);
                        ruta.setConsumoCombustible(consumo);
                        totalConsumo += consumo;
                        break;
                    }
                }
            }
        }
        return totalConsumo;
    }

    private int calcularPedidosEntregados(List<Ruta> rutas) {
        return rutas.stream().mapToInt(ruta -> ruta.getPedidosAsignados().size()).sum();
    }

    public String iniciarAlgoritmoDiario(AlgoritmoSimpleRequest request){
        List<Camion> camiones = dataRepository.obtenerCamiones();
        Mapa mapa = dataRepository.obtenerMapa();

        List<Pedido> todosPedidos = dataRepository.obtenerPedidos();

        LocalDateTime hoy = LocalDateTime.now();
        List<Pedido> pedidosHoy = todosPedidos.stream()
                .filter(pedido -> esMismoDia(pedido.getHoraRecepcion(), hoy))
                .collect(Collectors.toList());

        log.info("Planificando rutas para {} pedidos del día de hoy", pedidosHoy.size());

        // Si no hay pedidos hoy, no iniciar algoritmo
        if (pedidosHoy.isEmpty()) {
            throw new IllegalStateException("No hay pedidos para el día de hoy");
        }

        // Usar la implementación actual pero con los pedidos filtrados
        return iniciarEjecucionAlgoritmo(
                request.getTipoAlgoritmo(),
                camiones,
                pedidosHoy,
                mapa,
                hoy,
                EscenarioSimulacion.DIA_A_DIA,
                request.getTamañoPoblacion(),
                request.getNumGeneraciones(),
                request.getTasaMutacion(),
                request.getTasaCruce(),
                request.getElitismo(),
                request.getNumParticulas(),
                request.getNumIteraciones(),
                request.getW(),
                request.getC1(),
                request.getC2()
        );
    }


    /**
     * Inicia una ejecución del algoritmo considerando pedidos de la semana actual
     */
    public String iniciarAlgoritmoSemanal(AlgoritmoSimpleRequest request) {
        // Obtener datos del repositorio
        List<Camion> camiones = dataRepository.obtenerCamiones();
        Mapa mapa = dataRepository.obtenerMapa();
    
        // Obtener todos los pedidos
        List<Pedido> todosPedidos = dataRepository.obtenerPedidos();
    
        // Filtrar pedidos de la semana actual
        LocalDateTime hoy = LocalDateTime.now();
        LocalDateTime inicioDeSemana = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime finDeSemana = inicioDeSemana.plusDays(7);
    
        List<Pedido> pedidosSemana = todosPedidos.stream()
                .filter(pedido -> {
                    LocalDateTime fechaPedido = pedido.getHoraRecepcion();
                    return !fechaPedido.isBefore(inicioDeSemana) && fechaPedido.isBefore(finDeSemana);
                })
                .collect(Collectors.toList());
    
        log.info("Planificando rutas para {} pedidos de la semana con algoritmo {}", 
                 pedidosSemana.size(), request.getTipoAlgoritmo());
    
        // Si no hay pedidos en la semana, no iniciar algoritmo
        if (pedidosSemana.isEmpty()) {
            throw new IllegalStateException("No hay pedidos para la semana actual");
        }
    
        // Usar la implementación actual pero con los pedidos filtrados
        return iniciarEjecucionAlgoritmo(
                request.getTipoAlgoritmo(),
                camiones,
                pedidosSemana,
                mapa,
                hoy,
                EscenarioSimulacion.SEMANAL, // Usar escenario semanal
                request.getTamañoPoblacion(),
                request.getNumGeneraciones(),
                request.getTasaMutacion(),
                request.getTasaCruce(),
                request.getElitismo(),
                request.getNumParticulas(),
                request.getNumIteraciones(),
                request.getW(),
                request.getC1(),
                request.getC2()
        );
    }

    private boolean esMismoDia(LocalDateTime fecha1,LocalDateTime fecha2){
        return fecha1.getYear() == fecha2.getYear() &&
                fecha1.getDayOfYear() == fecha2.getDayOfYear();
    }

    /**
     * Inicia una ejecución del algoritmo genético considerando solo pedidos del día actual
     */
    public String iniciarAlgoritmoGeneticoDiario(AlgoritmoSimpleRequest request) {
        // Obtener datos del repositorio
        List<Camion> camiones = dataRepository.obtenerCamiones();
        Mapa mapa = dataRepository.obtenerMapa();

        // Obtener todos los pedidos
        List<Pedido> todosPedidos = dataRepository.obtenerPedidos();

        // Filtrar solo pedidos de hoy
        LocalDateTime hoy = LocalDateTime.now();
        List<Pedido> pedidosHoy = todosPedidos.stream()
                .filter(pedido -> esMismoDia(pedido.getHoraRecepcion(), hoy))
                .collect(Collectors.toList());

        log.info("Planificando rutas para {} pedidos del día de hoy con algoritmo genético", pedidosHoy.size());

        // Si no hay pedidos hoy, no iniciar algoritmo
        if (pedidosHoy.isEmpty()) {
            throw new IllegalStateException("No hay pedidos para el día de hoy");
        }

        // Forzar tipo de algoritmo a GENETICO
        request.setTipoAlgoritmo("GENETICO");

        // Usar la implementación actual pero con los pedidos filtrados
        return iniciarEjecucionAlgoritmo(
                "GENETICO", // Forzar algoritmo genético
                camiones,
                pedidosHoy,
                mapa,
                hoy,
                EscenarioSimulacion.DIA_A_DIA,
                request.getTamañoPoblacion(),
                request.getNumGeneraciones(),
                request.getTasaMutacion(),
                request.getTasaCruce(),
                request.getElitismo(),
                null, // Parámetros PSO no aplican
                null,
                null,
                null,
                null
        );
    }
}