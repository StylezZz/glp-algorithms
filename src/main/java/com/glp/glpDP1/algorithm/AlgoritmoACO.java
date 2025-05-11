package com.glp.glpDP1.algorithm;

import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del algoritmo de Optimización de Colonia de Hormigas (ACO)
 * para la optimización de rutas de distribución de GLP considerando averías y mantenimientos
 */
@Getter
@Setter
public class AlgoritmoACO {

    // Parámetros del algoritmo
    private int numHormigas;
    private int numIteraciones;
    private double alfa;     // Importancia de las feromonas
    private double beta;     // Importancia de la heurística (distancia/consumo)
    private double rho;      // Tasa de evaporación de feromonas
    private double q0;       // Parámetro de exploración vs explotación
    private double inicialFeromona; // Valor inicial de feromonas
    private int iteracionActual;

    // Datos del problema
    private List<Camion> camionesDisponibles;
    private List<Pedido> pedidosPendientes;
    private Mapa mapa;
    private LocalDateTime momentoActual;

    // Resultados
    private List<Ruta> mejorSolucion;
    private double mejorFitness;

    // Matriz de feromonas y heurísticas
    private Map<Ubicacion, Map<Ubicacion, Double>> matrizFeromonas;
    private Map<Ubicacion, Map<Ubicacion, Double>> matrizHeuristica;
    private List<Ubicacion> ubicacionesRelevantes;
    private Map<Ubicacion, List<Pedido>> pedidosPorUbicacion;

    private double feromonaMinima;
    private double feromonaMaxima;

    private List<Double> historialFitness = new ArrayList<>();

    /**
     * Constructor con parámetros predeterminados
     */
    public AlgoritmoACO() {
        this(30, 100, 1.0, 2.0, 0.2, 0.9, 0.1);
    }

    /**
     * Constructor con parámetros personalizados
     *
     * @param numHormigas     Número de hormigas en la colonia
     * @param numIteraciones  Número máximo de iteraciones
     * @param alfa            Importancia de las feromonas
     * @param beta            Importancia de la heurística
     * @param rho             Tasa de evaporación de feromonas
     * @param q0              Parámetro de exploración vs explotación (0-1)
     * @param inicialFeromona Valor inicial de feromonas
     */
    public AlgoritmoACO(int numHormigas, int numIteraciones, double alfa, double beta,
                        double rho, double q0, double inicialFeromona) {
        this.numHormigas = numHormigas;
        this.numIteraciones = numIteraciones;
        this.alfa = alfa;
        this.beta = beta;
        this.rho = rho;
        this.q0 = q0;
        this.inicialFeromona = inicialFeromona;
    }

    /**
     * Ejecuta el algoritmo ACO para encontrar la mejor asignación de rutas
     *
     * @param camiones Lista de camiones disponibles
     * @param pedidos  Lista de pedidos pendientes
     * @param mapa     Mapa de la ciudad
     * @param momento  Momento actual para la planificación
     * @return Lista de rutas optimizadas
     */
    public List<Ruta> optimizarRutas(List<Camion> camiones, List<Pedido> pedidos,
                                     Mapa mapa, LocalDateTime momento) {
        this.camionesDisponibles = filtrarCamionesDisponibles(camiones);
        this.pedidosPendientes = preprocesarPedidos(pedidos);
        this.mapa = mapa;
        this.momentoActual = momento;
        this.iteracionActual = 0;

        // Si no hay camiones disponibles o pedidos pendientes, retornar lista vacía
        if (camionesDisponibles.isEmpty() || pedidosPendientes.isEmpty()) {
            return new ArrayList<>();
        }

        // Inicializar matrices de feromonas y heurística
        inicializarMatrices();

        // Inicializar la mejor solución conocida
        mejorSolucion = null;
        mejorFitness = Double.MAX_VALUE;

        // Ciclo principal del algoritmo ACO
        for (int iteracion = 0; iteracion < numIteraciones; iteracion++) {
            this.iteracionActual = iteracion;
            List<Solucion> soluciones = new ArrayList<>();

            // Cada hormiga construye una solución
            for (int hormiga = 0; hormiga < numHormigas; hormiga++) {
                double q0Actual = calcularQ0Adaptativo(iteracion);
                Solucion solucion = construirSolucion(q0Actual);
                soluciones.add(solucion);

                // Actualizar mejor solución global si es necesario
                if (solucion.fitness < mejorFitness) {
                    mejorSolucion = solucion.rutas;
                    mejorFitness = solucion.fitness;
                    System.out.println("Iter " + iteracion + ", Hormiga " + hormiga + ": Nuevo mejor fitness = " + mejorFitness);
                    historialFitness.add(mejorFitness);
                }
            }

            // Actualizar feromonas
            actualizarFeromonas(soluciones);

            // Verificar convergencia
            if (iteracion > 20 && haConvergido()) {
                System.out.println("Convergencia alcanzada en iteración " + iteracion);
                break;
            }
        }

        return mejorSolucion != null ? mejorSolucion : new ArrayList<>();
    }

    /**
     * Registra un evento en la ruta
     */
    private void registrarEvento(Ruta ruta, EventoRuta.TipoEvento tipo,
                                 LocalDateTime momento, Ubicacion ubicacion) {
        String descripcion = "";
        switch (tipo) {
            case INICIO:
                descripcion = "Inicio de ruta";
                break;
            case ENTREGA:
                descripcion = "Entrega realizada";
                break;
            case RECARGA_COMBUSTIBLE:
                descripcion = "Recarga de combustible";
                break;
            case FIN:
                descripcion = "Regreso al almacén central";
                break;
            default:
                descripcion = tipo.toString();
        }

        ruta.registrarEvento(tipo, momento, ubicacion, descripcion);
    }

    /**
     * Preprocesa los pedidos dividiéndolos si son muy grandes
     */
    private List<Pedido> preprocesarPedidos(List<Pedido> pedidosOriginales) {
//        List<Pedido> pedidosOrdenados = pedidosOriginales.stream()
//                .sorted(Comparator.comparing(Pedido::getHoraRecepcion))
//                .collect(Collectors.toList());
        List<Pedido> pedidosProcesados = new ArrayList<>();
        for (Pedido pedido : pedidosOriginales) {
            if (pedido.getCantidadGLP() <= 12.0) {
                pedidosProcesados.add(pedido);
                continue;
            }

            double cantidadRestante = pedido.getCantidadGLP();
            int contador = 1;

            double sizeMaximoParte;
            if (cantidadRestante > 40.0) {
                sizeMaximoParte = 25.0; // Pedidos muy grandes en partes de 13m³
            } else if (cantidadRestante > 25.0) {
                sizeMaximoParte = 15.0; // Pedidos grandes en partes de 15m³
            } else {
                sizeMaximoParte = 10.0; // Resto en partes de 12m³
            }
            String idPedido = UUID.randomUUID().toString();
            while (cantidadRestante > 0) {
                double cantidadParte = Math.min(cantidadRestante, sizeMaximoParte);
                cantidadRestante -= cantidadParte;

                Pedido pedidoParte = new Pedido(
                        idPedido,
                        pedido.getIdCliente() + "_parte" + contador,
                        pedido.getUbicacion(),
                        cantidadParte,
                        pedido.getHoraRecepcion(),
                        (int) pedido.getTiempoLimiteEntrega().toHours()
                );

                pedidosProcesados.add(pedidoParte);
                contador++;
            }
        }
        return pedidosProcesados;
    }

    /**
     * Filtra los camiones que están disponibles para asignación
     * considerando averías y mantenimientos
     */
    private List<Camion> filtrarCamionesDisponibles(List<Camion> camiones) {
        // Actualizar estados de camiones según mantenimiento y averías
        for (Camion camion : camiones) {
            camion.actualizarEstado(momentoActual);
        }
        return camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE)
                .filter(c -> !estaEnMantenimientoProgramado(c))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un camión tiene mantenimiento programado pronto
     */
    private boolean estaEnMantenimientoProgramado(Camion camion) {
        // Si tiene mantenimiento en las próximas 12 horas, considerarlo no disponible
        if (camion.getFechaProximoMantenimiento() != null) {
            Duration hastaMantenimiento = Duration.between(
                    momentoActual, camion.getFechaProximoMantenimiento());
            return hastaMantenimiento.toHours() < 12;
        }
        return false;
    }

    /**
     * Inicializa las matrices de feromonas y heurística
     */
    private void inicializarMatrices() {
        // Crear lista de ubicaciones relevantes (UN SOLO VEZ)
        ubicacionesRelevantes = new ArrayList<>();

        // Agregar almacenes
        ubicacionesRelevantes.add(mapa.obtenerAlmacenCentral().getUbicacion());
        mapa.obtenerAlmacenesIntermedios().forEach(a ->
                ubicacionesRelevantes.add(a.getUbicacion()));

        // Agregar ubicaciones de pedidos (sin duplicados)
        for (Pedido pedido : pedidosPendientes) {
            if (!ubicacionesRelevantes.contains(pedido.getUbicacion())) {
                ubicacionesRelevantes.add(pedido.getUbicacion());
            }
        }

        // Agregar ubicaciones iniciales de camiones
        for (Camion camion : camionesDisponibles) {
            if (!ubicacionesRelevantes.contains(camion.getUbicacionActual())) {
                ubicacionesRelevantes.add(camion.getUbicacionActual());
            }
        }

        // Agrupar pedidos por ubicación - AGREGAR VALIDACIÓN
        pedidosPorUbicacion = new HashMap<>();
        if (pedidosPendientes != null && !pedidosPendientes.isEmpty()) {
            for (Pedido pedido : pedidosPendientes) {
                pedidosPorUbicacion.computeIfAbsent(pedido.getUbicacion(), k -> new ArrayList<>())
                        .add(pedido);
            }
        }

        // Inicializar matrices con TODAS las ubicaciones relevantes
        matrizFeromonas = new HashMap<>();
        matrizHeuristica = new HashMap<>();

        for (Ubicacion origen : ubicacionesRelevantes) {
            Map<Ubicacion, Double> feromonaDesde = new HashMap<>();
            Map<Ubicacion, Double> heuristicaDesde = new HashMap<>();

            for (Ubicacion destino : ubicacionesRelevantes) {
                // Permitir origen == destino, solo asignar valor 0 o mínimo
                double valorInicial = origen.equals(destino) ? 0.0 : inicialFeromona;
                feromonaDesde.put(destino, valorInicial);

                // Para heurística, si origen == destino, usar un valor alto para evitarlo
                double distancia = origen.distanciaA(destino);
                double valorHeuristica = distancia == 0 ? 0.0 : 1.0 / (distancia + 0.1);
                heuristicaDesde.put(destino, valorHeuristica);
            }

            matrizFeromonas.put(origen, feromonaDesde);
            matrizHeuristica.put(origen, heuristicaDesde);
        }

        // Inicializar límites MMAS
        feromonaMaxima = 1.0 / inicialFeromona;
        feromonaMinima = feromonaMaxima * 0.1;
    }

    /**
     * Construye una solución completa (una hormiga)
     */
    private Solucion construirSolucion(double q0Actual) {
        List<Ruta> rutas = new ArrayList<>();
        Set<Pedido> pedidosEntregados = new HashSet<>();

        // Para cada camión construir su ruta completa
        for (Camion camion : camionesDisponibles) {
            Ruta ruta = construirRutaParaCamion(camion, pedidosEntregados, q0Actual);
            if (ruta != null && !ruta.getPedidosAsignados().isEmpty()) {
                rutas.add(ruta);
            }
        }

        // Calcular fitness
        double fitness = calcularFitness(rutas);

        return new Solucion(rutas, fitness);
    }

    /**
     * Construye una ruta completa para un camión específico
     */
    private Ruta construirRutaParaCamion(Camion camion, Set<Pedido> pedidosEntregados, double q0) {
        Ruta ruta = new Ruta(camion.getCodigo(), camion.getUbicacionActual());
        ruta.setDestino(mapa.obtenerAlmacenCentral().getUbicacion());

        Ubicacion ubicacionActual = camion.getUbicacionActual();
        LocalDateTime tiempoActual = momentoActual;
        double combustibleActual = camion.getNivelCombustibleActual();
        double glpActual = 0; // Camión vacío al inicio

        // Seguir construyendo la ruta hasta que no sea factible continuar
        while (true) {
            Ubicacion proximaUbicacion = elegirProximaUbicacion(
                    ubicacionActual,
                    tiempoActual,
                    pedidosEntregados,
                    q0,
                    camion,
                    glpActual,
                    combustibleActual
            );

            if (proximaUbicacion == null) {
                // No hay más ubicaciones factibles
                break;
            }

            // Calcular ruta A* a la próxima ubicación
            List<Ubicacion> rutaHastaProxima = mapa.encontrarRutaConTiempo(
                    ubicacionActual,
                    proximaUbicacion,
                    tiempoActual,
                    camion.getVelocidadPromedio()
            );

            if (rutaHastaProxima.isEmpty()) {
                // No se puede llegar
                break;
            }

            // Calcular distancia y consumo
            int distancia = 0;
            for (int i = 0; i < rutaHastaProxima.size() - 1; i++) {
                distancia += rutaHastaProxima.get(i).distanciaA(rutaHastaProxima.get(i + 1));
            }

            double consumo = camion.calcularConsumoCombustible(distancia);

            // Verificar si hay suficiente combustible
            if (combustibleActual < consumo) {  // CORREGIDO: era 'combustible'
                // Necesita recarga, ir al almacén más cercano
                Almacen almacenCercano = mapa.obtenerAlmacenMasCercano(ubicacionActual);  // CORREGIDO: ahora declarado
                proximaUbicacion = almacenCercano.getUbicacion();

                rutaHastaProxima = mapa.encontrarRutaConTiempo(
                        ubicacionActual,
                        proximaUbicacion,
                        tiempoActual,
                        camion.getVelocidadPromedio()
                );

                if (rutaHastaProxima.isEmpty()) {
                    break;
                }
            }

            // Avanzar tiempo
            double horasViaje = distancia / camion.getVelocidadPromedio();
            tiempoActual = tiempoActual.plusMinutes((long) (horasViaje * 60));

            // Actualizar combustible
            combustibleActual -= consumo;

            // Procesar entrega/recarga en la ubicación
            Ubicacion finalProximaUbicacion = proximaUbicacion;
            boolean esAlmacen = mapa.getAlmacenes().stream()
                    .anyMatch(a -> a.getUbicacion().equals(finalProximaUbicacion));

            if (esAlmacen) {  // CORREGIDO: verificar si es almacén
                // Recargar combustible
                combustibleActual = camion.getCapacidadTanqueCombustible();
                registrarEvento(ruta, EventoRuta.TipoEvento.RECARGA_COMBUSTIBLE,
                        tiempoActual, proximaUbicacion);
            } else {
                // Procesar pedidos en esta ubicación
                List<Pedido> pedidosAqui = pedidosPorUbicacion.get(proximaUbicacion);
                if (pedidosAqui != null) {
                    for (Pedido pedido : pedidosAqui) {
                        if (!pedidosEntregados.contains(pedido) &&
                                glpActual + pedido.getCantidadGLP() <= camion.getCapacidadTanqueGLP() &&
                                !pedido.estaRetrasado(tiempoActual)) {

                            ruta.agregarPedido(pedido);
                            pedidosEntregados.add(pedido);
                            glpActual += pedido.getCantidadGLP();

                            registrarEvento(ruta, EventoRuta.TipoEvento.ENTREGA,
                                    tiempoActual, proximaUbicacion);
                        }
                    }
                }
            }

            ubicacionActual = proximaUbicacion;
        }

        // Regresar al almacén central
        List<Ubicacion> rutaRegreso = mapa.encontrarRutaConTiempo(
                ubicacionActual,
                mapa.obtenerAlmacenCentral().getUbicacion(),
                tiempoActual,
                camion.getVelocidadPromedio()
        );

        if (!rutaRegreso.isEmpty()) {
            // Calcular tiempo de regreso y actualizar
            int distanciaRegreso = 0;
            for (int i = 0; i < rutaRegreso.size() - 1; i++) {
                distanciaRegreso += rutaRegreso.get(i).distanciaA(rutaRegreso.get(i + 1));
            }
            double horasRegreso = distanciaRegreso / camion.getVelocidadPromedio();
            tiempoActual = tiempoActual.plusMinutes((long) (horasRegreso * 60));

            registrarEvento(ruta, EventoRuta.TipoEvento.FIN,
                    tiempoActual, mapa.obtenerAlmacenCentral().getUbicacion());
        }

        return ruta;
    }

    /**
     * Elige la próxima ubicación a visitar basado en feromonas y heurística
     */
    private Ubicacion elegirProximaUbicacion(Ubicacion actual,
                                             LocalDateTime tiempo,
                                             Set<Pedido> pedidosEntregados,
                                             double q0,
                                             Camion camion,
                                             double glpActual,
                                             double combustibleActual) {

        Map<Ubicacion, Double> feromonasDesdeActual = matrizFeromonas.get(actual);
        Map<Ubicacion, Double> heuristicaDesdeActual = matrizHeuristica.get(actual);

        if (feromonasDesdeActual == null || heuristicaDesdeActual == null) {
            return null;
        }

        // Filtrar ubicaciones factibles
        List<Ubicacion> ubicacionesFactibles = new ArrayList<>();
        for (Ubicacion destino : ubicacionesRelevantes) {
            if (esUbicacionFactible(actual, destino, tiempo, pedidosEntregados,
                    camion, glpActual, combustibleActual)) {
                ubicacionesFactibles.add(destino);
            }
        }

        if (ubicacionesFactibles.isEmpty()) {
            return null;
        }

        double random = Math.random();

        if (random < q0) {
            // Explotación: elegir la mejor opción
            Ubicacion mejorUbicacion = null;
            double mejorValor = -1;

            for (Ubicacion destino : ubicacionesFactibles) {
                double feromona = feromonasDesdeActual.get(destino);
                double heuristica = heuristicaDesdeActual.get(destino);
                double valor = Math.pow(feromona, alfa) * Math.pow(heuristica, beta);

                if (valor > mejorValor) {
                    mejorValor = valor;
                    mejorUbicacion = destino;
                }
            }

            return mejorUbicacion;
        } else {
            // Exploración: selección probabilística
            double sumaTotal = 0;
            Map<Ubicacion, Double> probabilidades = new HashMap<>();

            for (Ubicacion destino : ubicacionesFactibles) {
                double feromona = feromonasDesdeActual.get(destino);
                double heuristica = heuristicaDesdeActual.get(destino);
                double valor = Math.pow(feromona, alfa) * Math.pow(heuristica, beta);

                probabilidades.put(destino, valor);
                sumaTotal += valor;
            }

            // Normalizar probabilidades
            double r = Math.random() * sumaTotal;
            double acumulado = 0;

            for (Map.Entry<Ubicacion, Double> entry : probabilidades.entrySet()) {
                acumulado += entry.getValue();
                if (r <= acumulado) {
                    return entry.getKey();
                }
            }

            return ubicacionesFactibles.get(0); // Fallback
        }
    }

    /**
     * Verifica si una ubicación es factible visitar
     */
    private boolean esUbicacionFactible(Ubicacion origen,
                                        Ubicacion destino,
                                        LocalDateTime tiempo,
                                        Set<Pedido> pedidosEntregados,
                                        Camion camion,
                                        double glpActual,
                                        double combustibleActual) {

        // Verificar si hay ruta
        List<Ubicacion> ruta = mapa.encontrarRutaConTiempo(
                origen, destino, tiempo, camion.getVelocidadPromedio());

        if (ruta.isEmpty()) {
            return false;
        }

        // Calcular consumo necesario
        int distancia = 0;
        for (int i = 0; i < ruta.size() - 1; i++) {
            distancia += ruta.get(i).distanciaA(ruta.get(i + 1));
        }

        double consumo = camion.calcularConsumoCombustible(distancia);

        // Si es un almacén, siempre factible para recargar
        if (mapa.getAlmacenes().stream()
                .anyMatch(a -> a.getUbicacion().equals(destino))) {
            return true;
        }

        // Verificar si hay suficiente combustible
        if (combustibleActual < consumo) {
            return false;
        }

        // Verificar si hay pedidos pendientes en esta ubicación
        List<Pedido> pedidosAqui = pedidosPorUbicacion.get(destino);
        if (pedidosAqui == null) {
            return false;
        }

        boolean hayPedidosPendientes = false;
        for (Pedido pedido : pedidosAqui) {
            if (!pedidosEntregados.contains(pedido) &&
                    glpActual + pedido.getCantidadGLP() <= camion.getCapacidadTanqueGLP()) {

                // Calcular tiempo de llegada
                double horasViaje = distancia / camion.getVelocidadPromedio();
                LocalDateTime llegada = tiempo.plusMinutes((long) (horasViaje * 60));

                if (!pedido.estaRetrasado(llegada)) {
                    hayPedidosPendientes = true;
                    break;
                }
            }
        }

        return hayPedidosPendientes;
    }


    /**
     * Actualiza la matriz de feromonas basado en las soluciones
     */
    private void actualizarFeromonas(List<Solucion> soluciones) {
        // Evaporación de feromonas
        for (Ubicacion origen : matrizFeromonas.keySet()) {
            Map<Ubicacion, Double> feromonasDesde = matrizFeromonas.get(origen);
            for (Ubicacion destino : feromonasDesde.keySet()) {
                double feromonaActual = feromonasDesde.get(destino);
                feromonasDesde.put(destino, feromonaActual * (1 - rho));
            }
        }

        // Depósito de feromonas
        for (Solucion solucion : soluciones) {
            double deltaFeromona = 1.0 / (solucion.fitness + 0.1);

            // Depositar feromona en cada ruta
            for (Ruta ruta : solucion.rutas) {
                List<Ubicacion> nodosRuta = ruta.getSecuenciaNodos();
                for (int i = 0; i < nodosRuta.size() - 1; i++) {
                    Ubicacion origen = nodosRuta.get(i);
                    Ubicacion destino = nodosRuta.get(i + 1);

                    Map<Ubicacion, Double> feromonasDesde = matrizFeromonas.get(origen);
                    if (feromonasDesde != null && feromonasDesde.containsKey(destino)) {
                        double feromonaActual = feromonasDesde.get(destino);
                        feromonasDesde.put(destino, feromonaActual + deltaFeromona);
                    }
                }
            }
        }

        // Elitismo: reforzar la mejor solución global
        if (mejorSolucion != null) {
            double deltaMejor = 1.0 / (mejorFitness + 0.1);

            for (Ruta ruta : mejorSolucion) {
                List<Ubicacion> nodosRuta = ruta.getSecuenciaNodos();
                for (int i = 0; i < nodosRuta.size() - 1; i++) {
                    Ubicacion origen = nodosRuta.get(i);
                    Ubicacion destino = nodosRuta.get(i + 1);

                    Map<Ubicacion, Double> feromonasDesde = matrizFeromonas.get(origen);
                    if (feromonasDesde != null && feromonasDesde.containsKey(destino)) {
                        double feromonaActual = feromonasDesde.get(destino);
                        feromonasDesde.put(destino, feromonaActual + deltaMejor * 2);
                    }
                }
            }
        }

        // Aplicar límites MMAS
        for (Ubicacion origen : matrizFeromonas.keySet()) {
            Map<Ubicacion, Double> feromonasDesde = matrizFeromonas.get(origen);
            for (Ubicacion destino : feromonasDesde.keySet()) {
                double feromona = feromonasDesde.get(destino);
                if (feromona > feromonaMaxima) {
                    feromonasDesde.put(destino, feromonaMaxima);
                } else if (feromona < feromonaMinima) {
                    feromonasDesde.put(destino, feromonaMinima);
                }
            }
        }
    }

    /**
     * Calcula el fitness de una solución (menor es mejor)
     */
    private double calcularFitness(List<Ruta> rutas) {
        double consumoTotal = 0.0;      // Consumo total de combustible
        double distanciaTotal = 0.0;    // Distancia total recorrida
        double retrasosTotal = 0.0;     // Suma de retrasos (en minutos)
        double pedidosNoAsignados = pedidosPendientes.size(); // Inicialmente, todos no asignados
        double sobrecargaTotal = 0.0;   // Suma de sobrecargas de GLP en camiones
        double penalizacionAverias = 0.0; // Penalización por probabilidad de averías
        double bonificacionAlmacenPrincipal = 0.0;

        Ubicacion ubicacionAlmacenPrincipal = mapa.obtenerAlmacenCentral().getUbicacion();
        // Conjunto de pedidos asignados
        Set<String> pedidosAsignadosIds = new HashSet<>();
        List<Double> porcentajesUso = new ArrayList<>();

        // Evaluar cada ruta
        for (Ruta ruta : rutas) {
            // Obtener el camión asignado a esta ruta
            Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
            if (camion == null) continue;

            // Calcular consumo de combustible
            double consumo = ruta.calcularConsumoCombustible(camion);
            consumoTotal += consumo;
            ruta.setConsumoCombustible(consumo);

            // Sumar distancia total
            distanciaTotal += ruta.getDistanciaTotal();

            // Verificar capacidad GLP
            double glpTotal = 0;
            for (Pedido p : ruta.getPedidosAsignados()) {
                glpTotal += p.getCantidadGLP();
                // Extraer ID base si es un pedido dividido
                String idBase = p.getId().contains("_parte")
                        ? p.getId().substring(0, p.getId().indexOf("_parte"))
                        : p.getId();
                pedidosAsignadosIds.add(idBase);
            }

            if (glpTotal > camion.getCapacidadTanqueGLP()) {
                sobrecargaTotal += (glpTotal - camion.getCapacidadTanqueGLP());
            }

            List<Ubicacion> nodos = ruta.getSecuenciaNodos();
            long visitasAlmacenPrincipal = nodos.stream()
                    .filter(nodo -> nodo.equals(ubicacionAlmacenPrincipal)).count();

            if (visitasAlmacenPrincipal > 0 && glpTotal > camion.getCapacidadTanqueGLP()) {
                // La ruta necesita reabastecimiento y lo hace correctamente
                double eficienciaReabastecimiento = Math.min(1.0, visitasAlmacenPrincipal /
                        (Math.ceil(glpTotal / camion.getCapacidadTanqueGLP()) - 1));

                // Mayor bonificación para rutas que utilizan el reabastecimiento de manera eficiente
                bonificacionAlmacenPrincipal += glpTotal * 0.5 * eficienciaReabastecimiento;
            }

            // Calcular retrasos potenciales
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                // Estimar tiempo de entrega basado en distancia y velocidad
                Ubicacion origen = camion.getUbicacionActual();
                int distanciaAlPedido = origen.distanciaA(pedido.getUbicacion());
                double horasViaje = distanciaAlPedido / 50.0; // 50 km/h velocidad promedio

                LocalDateTime entregaEstimada = momentoActual.plusMinutes((long) (horasViaje * 60));

                // Si entrega estimada es posterior a la hora límite, hay retraso
                if (entregaEstimada.isAfter(pedido.getHoraLimiteEntrega())) {
                    Duration retraso = Duration.between(pedido.getHoraLimiteEntrega(), entregaEstimada);
                    retrasosTotal += retraso.toMinutes();
                }
            }
        }

        // Actualizar pedidos no asignados
        Set<String> idsBasePendientes = new HashSet<>();
        for (Pedido pedido : pedidosPendientes) {
            String idBase = pedido.getId().contains("_parte")
                    ? pedido.getId().substring(0, pedido.getId().indexOf("_parte"))
                    : pedido.getId();

            if (!pedidosAsignadosIds.contains(idBase)) {
                idsBasePendientes.add(idBase);
            }
        }
        pedidosNoAsignados = idsBasePendientes.size();

        double penalizacionDesviacion = 0;
        if (porcentajesUso.size() > 1) {
            double media = porcentajesUso.stream().mapToDouble(d -> d).average().orElse(0);
            double sumaCuadrados = porcentajesUso.stream().mapToDouble(p -> Math.pow(p - media, 2)).sum();
            double desviacionCarga = Math.sqrt(sumaCuadrados / porcentajesUso.size());
            penalizacionDesviacion = desviacionCarga * 500;  // Factor de penalización ajustable
        }

        // Calcular fitness final (ponderado) con mayor penalización por pedidos no asignados
        double fitness = 0.02 * distanciaTotal +
                0.03 * retrasosTotal +
                0.80 * (pedidosNoAsignados * 1000) +  // AUMENTAR significativamente la penalización
                0.03 * (sobrecargaTotal * 1000) +     // Reducir penalización por sobrecarga
                0.02 * penalizacionAverias -
                0.05 * bonificacionAlmacenPrincipal;       // Reducir penalización por averías

        return fitness;
    }

    private double calcularQ0Adaptativo(int iteracion) {
        double q0Base = this.q0;
        double q0Max = 0.95;
        double progreso = (double) iteracion / numIteraciones;

        return q0Base + (q0Max - q0Base) * (1.0 / (1.0 + Math.exp(-12 * progreso + 6)));
    }

    /**
     * Verifica si el algoritmo ha convergido
     */
    private boolean haConvergido() {
        // Si estamos en iteraciones tempranas, no permitir convergencia
        if (iteracionActual < 40) {
            return false; // Forzar al menos 40 iteraciones
        }

        // Comprobar si la matriz de feromonas ha convergido
        int filasConvergidas = 0;
        double umbralConvergencia = 0.95;

        for (Map.Entry<Ubicacion, Map<Ubicacion, Double>> entrada : matrizFeromonas.entrySet()) {
            Map<Ubicacion, Double> feromonasDesde = entrada.getValue();

            double max = 0;
            double segundoMax = 0;

            for (Double feromona : feromonasDesde.values()) {
                if (feromona > max) {
                    segundoMax = max;
                    max = feromona;
                } else if (feromona > segundoMax) {
                    segundoMax = feromona;
                }
            }

            if (segundoMax == 0 || max > segundoMax * umbralConvergencia) {
                filasConvergidas++;
            }
        }

        boolean convergenciaFeromonas = filasConvergidas >= matrizFeromonas.size() * 0.85;

        boolean estabilidadFitness = false;

        if (historialFitness.size() >= 10) {
            double ultimoFitness = historialFitness.get(historialFitness.size() - 1);
            double desviacionMax = 0.001 * ultimoFitness;

            estabilidadFitness = true;
            for (int i = historialFitness.size() - 10; i < historialFitness.size(); i++) {
                if (Math.abs(historialFitness.get(i) - ultimoFitness) > desviacionMax) {
                    estabilidadFitness = false;
                    break;
                }
            }
        }

        // Necesitamos una proporción mayor para considerar convergencia
        return convergenciaFeromonas && estabilidadFitness;
    }

    /**
     * Obtiene un camión por su código
     */
    private Camion getCamionPorCodigo(String codigo) {
        return camionesDisponibles.stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    /**
     * Clase interna para representar una solución (una hormiga)
     */
    private record Solucion(List<Ruta> rutas, double fitness) {
    }
}