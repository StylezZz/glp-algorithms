package com.glp.glpDP1.algorithm;

import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import com.glp.glpDP1.domain.enums.TipoIncidente;
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
@Getter @Setter
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
    private double[][] matrizFeromonas;
    private double[][] matrizHeuristica;

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
     * @param numHormigas Número de hormigas en la colonia
     * @param numIteraciones Número máximo de iteraciones
     * @param alfa Importancia de las feromonas
     * @param beta Importancia de la heurística
     * @param rho Tasa de evaporación de feromonas
     * @param q0 Parámetro de exploración vs explotación (0-1)
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
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa de la ciudad
     * @param momento Momento actual para la planificación
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

        // Optimizar las rutas finales (recargas, etc.)
        if (mejorSolucion != null) {
            for (Ruta ruta : mejorSolucion) {
                Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
                if (camion != null) {
                    //ruta.optimizarSecuenciaConBloqueos(mapa,momentoActual);
                    ruta.optimizarConRecargas(mapa, camion);
                }
            }
        }

        return mejorSolucion != null ? mejorSolucion : new ArrayList<>();
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
                sizeMaximoParte = 13.0; // Pedidos muy grandes en partes de 13m³
            } else if (cantidadRestante > 25.0) {
                sizeMaximoParte = 15.0; // Pedidos grandes en partes de 15m³
            } else {
                sizeMaximoParte = 12.0; // Resto en partes de 12m³
            }
            while (cantidadRestante > 0) {
                double cantidadParte = Math.min(cantidadRestante, sizeMaximoParte);
                cantidadRestante -= cantidadParte;

                Pedido pedidoParte = new Pedido(
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

    private double calcularPenalizacionBloqueos(Ubicacion origen, Ubicacion destino, LocalDateTime momento) {
        // Factor de penalización base
        double penalizacion = 1.0;

        // Verificar cada posible bloqueo
        for (Bloqueo bloqueo : mapa.getBloqueos()) {
            // Si el tramo está bloqueado, aumentar penalización
            if (bloqueo.tramoBloqueado(origen, destino, momento)) {
                penalizacion *= 10.0; // Penalización fuerte
                return penalizacion; // Retornar inmediatamente
            }

            // Si algún nodo está bloqueado, también penalizar
            for (Ubicacion nodo : bloqueo.getNodosBloqueados()) {
                // Si está cerca del origen o destino (menos de 3 unidades)
                if (origen.distanciaA(nodo) <= 3 || destino.distanciaA(nodo) <= 3) {
                    penalizacion *= 2.0;
                }
            }
        }

        return penalizacion;
    }


    /**
     * Inicializa las matrices de feromonas y heurística
     */
    private void inicializarMatrices() {
        int numPedidos = pedidosPendientes.size();
        int numCamiones = camionesDisponibles.size();

        // Matriz de feromonas: índice del pedido, índice del camión
        matrizFeromonas = new double[numPedidos][numCamiones + 1]; // +1 para la opción de no asignar

        // Matriz de heurística: inverso de distancia/consumo
        matrizHeuristica = new double[numPedidos][numCamiones + 1];

        // Inicializar feromonas con valor inicial
        for (int i = 0; i < numPedidos; i++) {
            Pedido pedido = pedidosPendientes.get(i);
            double horasLimite = pedido.getTiempoLimiteEntrega().toHours();
            double feromonaInicial = inicialFeromona;
            if(horasLimite<6){
                feromonaInicial*=2.0;
            }else{
                feromonaInicial*=1.5;
            }
            for (int j = 0; j <= numCamiones; j++) {
                matrizFeromonas[i][j] = feromonaInicial;
            }
        }

        // Calcular heurística basada en distancia/consumo
        for (int i = 0; i < numPedidos; i++) {
            Pedido pedido = pedidosPendientes.get(i);

            for (int j = 0; j < numCamiones; j++) {
                Camion camion = camionesDisponibles.get(j);

                // Calcular distancia
                int distancia = camion.getUbicacionActual().distanciaA(pedido.getUbicacion());

                // Calcular consumo estimado
                double consumoEstimado = camion.calcularConsumoCombustible(distancia);

                // Verificar capacidad
                boolean capacidadSuficiente = camion.getCapacidadTanqueGLP() >= pedido.getCantidadGLP();

                // Verificar combustible
                boolean combustibleSuficiente = camion.calcularDistanciaMaxima() >= distancia;

                // Calcular penalización por bloqueos
                double penalizacionBloqueos = calcularPenalizacionBloqueos(
                        camion.getUbicacionActual(), pedido.getUbicacion(), momentoActual);

                // Si no cumple con requisitos básicos, la heurística es muy baja
                if (!capacidadSuficiente || !combustibleSuficiente) {
                    matrizHeuristica[i][j] = 0.001;
                } else {
                    double valorBase = 1.0/(consumoEstimado+0.1);
                    double horasLimite = pedido.getTiempoLimiteEntrega().toHours();
                    double factorUrgencia = horasLimite < 8 ? 2.0: 1.0;
                    // Inversamente proporcional al consumo (menor consumo = mejor)
                    matrizHeuristica[i][j] = (valorBase*factorUrgencia)/penalizacionBloqueos;
                }
            }

            // Opción de no asignar (última columna)
            matrizHeuristica[i][numCamiones] = 0.001; // Desfavorecemos no asignar
        }
    }

    /**
     * Construye una solución completa (una hormiga)
     */
    private Solucion construirSolucion(double q0Actual) {
        int numPedidos = pedidosPendientes.size();
        int numCamiones = camionesDisponibles.size();

        // Asignación de pedidos a camiones (-1 significa no asignado)
        int[] asignaciones = new int[numPedidos];
        Arrays.fill(asignaciones, -1);

        // Lista de pedidos no asignados
        List<Integer> pedidosNoAsignados = new ArrayList<>();
        for (int i = 0; i < numPedidos; i++) {
            pedidosNoAsignados.add(i);
        }

        // Aseguramos que cada camión reciba al menos un pedido si es posible
        // usando nuestra versión mejorada
        asignarPedidosIniciales(asignaciones, pedidosNoAsignados);

        // Para cada pedido no asignado, decidir a qué camión asignarlo
        // Iterar múltiples veces para mejorar asignación
        for (int intento = 0; intento < 3 && !pedidosNoAsignados.isEmpty(); intento++) {
            List<Integer> pedidosRestantes = new ArrayList<>(pedidosNoAsignados);
            for (Integer indexPedido : pedidosRestantes) {
                // Decidir a qué camión asignarlo con nuestra estrategia mejorada
                int camionElegido = elegirCamion(indexPedido, asignaciones);

                // Si encontramos un camión válido (no -1), asignar pedido
                if (camionElegido >= 0 && camionElegido < numCamiones) {
                    asignaciones[indexPedido] = camionElegido;
                    pedidosNoAsignados.remove(indexPedido);
                }
            }
        }

        // Construir rutas a partir de asignaciones
        List<Ruta> rutas = construirRutas(asignaciones);

        // Calcular fitness
        double fitness = calcularFitness(rutas);

        return new Solucion(rutas, fitness, asignaciones);
    }

    /**
     * Asegura que cada camión reciba al menos un pedido inicial si es posible
     */
    private void asignarPedidosIniciales(int[] asignaciones, List<Integer> pedidosNoAsignados) {
        if (pedidosNoAsignados.isEmpty()) return;

        // 1. Identificar pedidos críticos (tiempo límite corto)
        List<Integer> pedidosCriticos = new ArrayList<>();
        for (Integer indice : pedidosNoAsignados) {
            Pedido pedido = pedidosPendientes.get(indice);
            // Considerar críticos los pedidos con menos de 6 horas de margen
            if (pedido.getTiempoLimiteEntrega().toHours() <= 6) {
                pedidosCriticos.add(indice);
            }
        }

        // 2. Asignar primero los pedidos críticos
        for (Integer indicePedido : pedidosCriticos) {
            // Encontrar el mejor camión para este pedido crítico
            int mejorCamion = -1;
            double mejorValor = -1;

            for (int i = 0; i < camionesDisponibles.size(); i++) {
                Camion camion = camionesDisponibles.get(i);
                Pedido pedido = pedidosPendientes.get(indicePedido);

                // Verificar capacidad
                if (camion.getCapacidadTanqueGLP() < pedido.getCantidadGLP()) continue;

                // Calcular valor combinado (favoreciendo camiones con más capacidad disponible)
                double capacidadRestante = camion.getCapacidadTanqueGLP() - pedido.getCantidadGLP();
                double distancia = camion.getUbicacionActual().distanciaA(pedido.getUbicacion());
                double valor = capacidadRestante - (distancia * 0.1);

                if (valor > mejorValor) {
                    mejorValor = valor;
                    mejorCamion = i;
                }
            }

            // Si encontramos un camión adecuado, asignar y remover de pendientes
            if (mejorCamion != -1) {
                asignaciones[indicePedido] = mejorCamion;
                pedidosNoAsignados.remove(Integer.valueOf(indicePedido));
            }
        }

        // 3. Asignar al menos un pedido a cada camión (para el resto de pedidos no críticos)
        List<Integer> camionesVacios = new ArrayList<>();
        for (int i = 0; i < camionesDisponibles.size(); i++) {
            // Verificar si el camión ya tiene algún pedido asignado
            boolean tieneAsignacion = false;
            for (int j = 0; j < asignaciones.length; j++) {
                if (asignaciones[j] == i) {
                    tieneAsignacion = true;
                    break;
                }
            }

            if (!tieneAsignacion) {
                camionesVacios.add(i);
            }
        }

        // Intentar asignar un pedido a cada camión vacío
        for (Integer indiceCamion : camionesVacios) {
            if (pedidosNoAsignados.isEmpty()) break;

            Camion camion = camionesDisponibles.get(indiceCamion);
            Integer mejorPedido = null;
            double mejorValor = -1;

            // Buscar el pedido más compatible para este camión
            for (Integer indicePedido : pedidosNoAsignados) {
                Pedido pedido = pedidosPendientes.get(indicePedido);

                // Verificar si el camión puede manejar este pedido
                if (camion.getCapacidadTanqueGLP() < pedido.getCantidadGLP()) continue;

                // Calcular valor basado en distancia y tiempo límite
                int distancia = camion.getUbicacionActual().distanciaA(pedido.getUbicacion());
                double horasLimite = pedido.getTiempoLimiteEntrega().toHours();

                // Priorizar pedidos cercanos con amplio margen de tiempo
                double valor = (100 - distancia) + (horasLimite * 2);

                if (valor > mejorValor) {
                    mejorValor = valor;
                    mejorPedido = indicePedido;
                }
            }

            // Si encontramos un pedido adecuado, asignar
            if (mejorPedido != null) {
                asignaciones[mejorPedido] = indiceCamion;
                pedidosNoAsignados.remove(mejorPedido);
            }
        }
    }

    /**
     * Elige a qué camión asignar un pedido basado en feromonas y heurística
     */
    private int elegirCamion(int indicePedido, int[] asignaciones) {
        int numCamiones = camionesDisponibles.size();
        double random = Math.random();

        // Regla de decisión pseudoaleatoria proporcional
        if (random < q0) {
            // Explotación: elegir la mejor opción
            double mejorValor = -1;
            int mejorCamion = -1;

            for (int j = 0; j <= numCamiones; j++) {
                double feromonaHeuristica =
                        Math.pow(matrizFeromonas[indicePedido][j], alfa) *
                                Math.pow(matrizHeuristica[indicePedido][j], beta);

                if (feromonaHeuristica > mejorValor) {
                    // Verificar restricciones de capacidad si es un camión
                    if (j < numCamiones) {
                        Camion camion = camionesDisponibles.get(j);
                        Pedido pedido = pedidosPendientes.get(indicePedido);

                        // Verificar capacidad GLP disponible con pedidos ya asignados
                        double glpYaAsignado = 0;
                        for (int k = 0; k < asignaciones.length; k++) {
                            if (asignaciones[k] == j) {
                                glpYaAsignado += pedidosPendientes.get(k).getCantidadGLP();
                            }
                        }

                        double capacidadDisponible = camion.getCapacidadTanqueGLP() - glpYaAsignado;
                        if (capacidadDisponible < pedido.getCantidadGLP()) {
                            continue;
                        }
                    }

                    mejorValor = feromonaHeuristica;
                    mejorCamion = j;
                }
            }

            return mejorCamion;
        } else {
            // Exploración: selección basada en probabilidad
            double[] probabilidades = new double[numCamiones + 1];
            double sumaTotal = 0;

            for (int j = 0; j <= numCamiones; j++) {
                double valor = Math.pow(matrizFeromonas[indicePedido][j], alfa) *
                        Math.pow(matrizHeuristica[indicePedido][j], beta);

                // Verificar restricciones para camiones
                if (j < numCamiones) {
                    Camion camion = camionesDisponibles.get(j);
                    Pedido pedido = pedidosPendientes.get(indicePedido);

                    // Verificar capacidad GLP con pedidos ya asignados
                    double glpYaAsignado = 0;
                    for (int k = 0; k < asignaciones.length; k++) {
                        if (asignaciones[k] == j) {
                            glpYaAsignado += pedidosPendientes.get(k).getCantidadGLP();
                        }
                    }

                    double capacidadDisponible = camion.getCapacidadTanqueGLP() - glpYaAsignado;
                    if (capacidadDisponible < pedido.getCantidadGLP()) {
                        valor = 0; // No es una opción viable
                    }
                }

                probabilidades[j] = valor;
                sumaTotal += valor;
            }

            // Normalizar probabilidades
            if (sumaTotal > 0) {
                for (int j = 0; j <= numCamiones; j++) {
                    probabilidades[j] /= sumaTotal;
                }
            } else {
                // Si no hay opciones viables, no asignar el pedido
                return numCamiones; // Índice de "no asignar"
            }

            // Selección basada en ruleta
            double r = Math.random();
            double acumulado = 0;

            for (int j = 0; j <= numCamiones; j++) {
                acumulado += probabilidades[j];
                if (r <= acumulado) {
                    return j;
                }
            }

            return numCamiones; // Por defecto, no asignar
        }
    }

    /**
     * Construye rutas a partir de asignaciones de pedidos a camiones
     */
    private List<Ruta> construirRutas(int[] asignaciones) {
        Map<String, Ruta> rutasPorCamion = new HashMap<>();

        // Para cada pedido, asignarlo a la ruta del camión correspondiente
        for (int i = 0; i < asignaciones.length; i++) {
            int indiceCamion = asignaciones[i];

            // Si el pedido no está asignado, continuar
            if (indiceCamion == -1 || indiceCamion >= camionesDisponibles.size()) {
                continue;
            }

            // Obtener el camión y el pedido
            Camion camion = camionesDisponibles.get(indiceCamion);
            Pedido pedido = pedidosPendientes.get(i);

            // Obtener o crear la ruta para este camión
            String codigoCamion = camion.getCodigo();
            Ruta ruta = rutasPorCamion.get(codigoCamion);

            if (ruta == null) {
                ruta = new Ruta(codigoCamion, camion.getUbicacionActual());
                ruta.setDestino(mapa.obtenerAlmacenCentral().getUbicacion());
                rutasPorCamion.put(codigoCamion, ruta);
            }

            // Añadir el pedido a la ruta
            ruta.agregarPedido(pedido);
        }

        // NUEVA SECCIÓN: Optimizar la carga inicial de cada camión
        for (Map.Entry<String, Ruta> entry : rutasPorCamion.entrySet()) {
            String codigoCamion = entry.getKey();
            Ruta ruta = entry.getValue();

            // Encontrar el camión correspondiente
            Camion camion = camionesDisponibles.stream()
                    .filter(c -> c.getCodigo().equals(codigoCamion))
                    .findFirst()
                    .orElse(null);

            if (camion != null) {
                // Calcular el GLP total necesario para esta ruta
                double glpNecesario = ruta.getPedidosAsignados().stream()
                        .mapToDouble(Pedido::getCantidadGLP)
                        .sum();

                // Limpiar el nivel actual de GLP (para asegurar valor exacto)
                camion.setNivelGLPActual(0.0);

                // Cargar el camión con la cantidad exacta de GLP necesaria (sin exceder su capacidad)
                double cargaOptima = Math.min(glpNecesario, camion.getCapacidadTanqueGLP());
                camion.cargarGLP(cargaOptima);

                // Si la ruta requiere más GLP del que el camión puede llevar en un viaje,
                // modificamos la optimización para incluir paradas en el almacén principal
                if (glpNecesario > camion.getCapacidadTanqueGLP()) {
                    ruta.setRequiereReabastecimiento(true);
                    // Marcamos para optimización especial
                }
            }
        }

        // Optimizar el orden de cada ruta
        for (Ruta ruta : rutasPorCamion.values()) {
            Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
            ruta.optimizarSecuenciaConBloqueos(mapa,momentoActual);
            double glpTotal = ruta.getPedidosAsignados().stream().mapToDouble(Pedido::getCantidadGLP).sum();

            if(camion!=null && glpTotal>camion.getCapacidadTanqueGLP()){
                ruta.setRequiereReabastecimiento(true);
                ruta.optimizarConRecargas(mapa,camion);
            }
        }

        return new ArrayList<>(rutasPorCamion.values());
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
                    .filter(nodo->nodo.equals(ubicacionAlmacenPrincipal)).count();

            if(visitasAlmacenPrincipal>0 && glpTotal > camion.getCapacidadTanqueGLP()){
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

                LocalDateTime entregaEstimada = momentoActual.plusMinutes((long)(horasViaje * 60));

                // Si entrega estimada es posterior a la hora límite, hay retraso
                if (entregaEstimada.isAfter(pedido.getHoraLimiteEntrega())) {
                    Duration retraso = Duration.between(pedido.getHoraLimiteEntrega(), entregaEstimada);
                    retrasosTotal += retraso.toMinutes();
                }
            }
        }

        // Actualizar pedidos no asignados
        pedidosNoAsignados =0;
        Set<String> idsBasePendientes = new HashSet<>();

        for(Pedido pedido:pedidosPendientes){
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
                0.90 * (pedidosNoAsignados * 1000) +  // AUMENTAR significativamente la penalización
                0.03 * (sobrecargaTotal * 1000) +     // Reducir penalización por sobrecarga
                0.02 * penalizacionAverias -
                0.05*bonificacionAlmacenPrincipal;       // Reducir penalización por averías

        return fitness;
    }

    private double calcularQ0Adaptativo(int iteracion){
        double q0Base = this.q0;
        double q0Max = 0.95;
        double progreso = (double) iteracion / numIteraciones;

        return q0Base + (q0Max -q0Base)*(1.0/(1.0+Math.exp(-12*progreso+6)));
    }

    private void actualizarLimitesMMAS(){
        if(mejorFitness>0){
            feromonaMaxima = 1.0/((1-rho)*mejorFitness);
            feromonaMinima = feromonaMaxima*0.1;
        }else{
            feromonaMaxima = 10.0;
            feromonaMinima = 0.01;
        }
    }

    /**
     * Actualiza la matriz de feromonas basado en las soluciones
     */
    private void actualizarFeromonas(List<Solucion> soluciones) {
        int numPedidos = pedidosPendientes.size();
        int numCamiones = camionesDisponibles.size();

        // Evaporación de feromonas
        for (int i = 0; i < numPedidos; i++) {
            for (int j = 0; j <= numCamiones; j++) {
                matrizFeromonas[i][j] *= (1 - rho);
                if (matrizFeromonas[i][j] < 0.01) {
                    matrizFeromonas[i][j] = 0.01; // Valor mínimo de feromona
                }
            }
        }

        // Depósito de feromonas por cada hormiga, proporcional a calidad de solución
        for (Solucion solucion : soluciones) {
            double deltaFeromona = 1.0 / (solucion.fitness + 0.1); // Inversamente proporcional al fitness

            // Aplicar depósito de feromonas en los caminos utilizados
            for (int i = 0; i < solucion.asignaciones.length; i++) {
                int j = solucion.asignaciones[i];
                if (j >= 0 && j <= numCamiones) {
                    matrizFeromonas[i][j] += deltaFeromona;
                }
            }
        }

        // Reforzar adicionalmente la mejor solución global (elitismo)
        if (mejorSolucion != null) {
            double deltaMejor = 1.0 / (mejorFitness + 0.1);

            // Determinar asignaciones en la mejor solución
            Map<String, Set<String>> mejorAsignacion = new HashMap<>();
            for (Ruta ruta : mejorSolucion) {
                Set<String> idsPedidos = ruta.getPedidosAsignados().stream()
                        .map(Pedido::getId)
                        .collect(Collectors.toSet());
                mejorAsignacion.put(ruta.getCodigoCamion(), idsPedidos);
            }

            // Reforzar estas asignaciones
            for (int i = 0; i < pedidosPendientes.size(); i++) {
                Pedido pedido = pedidosPendientes.get(i);

                for (int j = 0; j < camionesDisponibles.size(); j++) {
                    Camion camion = camionesDisponibles.get(j);
                    Set<String> pedidosAsignados = mejorAsignacion.get(camion.getCodigo());

                    if (pedidosAsignados != null && pedidosAsignados.contains(pedido.getId())) {
                        matrizFeromonas[i][j] += deltaMejor * 2; // Refuerzo adicional
                    }
                }
            }
        }

        for(int i=0;i<matrizFeromonas.length;i++){
            for(int j=0;j<matrizFeromonas[i].length;j++){
                if(matrizFeromonas[i][j]>feromonaMaxima){
                    matrizFeromonas[i][j]=feromonaMaxima;
                }else if(matrizFeromonas[i][j]<feromonaMinima){
                    matrizFeromonas[i][j]=feromonaMinima;
                }
            }
        }
    }

    /**
     * Verifica si el algoritmo ha convergido
     */
    private boolean haConvergido() {
        // Cambiar condición para que sea más difícil converger

        // Si estamos en iteraciones tempranas, no permitir convergencia
        if (iteracionActual < 40) {
            return false; // Forzar al menos 40 iteraciones
        }

        // Comprobar si la matriz de feromonas ha convergido
        int filasConvergidas = 0;
        double umbralConvergencia = 0.95; // Aumentado de 0.9 a 0.95

        for (int i = 0; i < matrizFeromonas.length; i++) {
            double max = 0;
            double segundoMax = 0;

            for (int j = 0; j < matrizFeromonas[i].length; j++) {
                if (matrizFeromonas[i][j] > max) {
                    segundoMax = max;
                    max = matrizFeromonas[i][j];
                } else if (matrizFeromonas[i][j] > segundoMax) {
                    segundoMax = matrizFeromonas[i][j];
                }
            }

            if (segundoMax == 0 || max > segundoMax * umbralConvergencia) {
                filasConvergidas++;
            }
        }

        boolean convergenciaFeromonas = filasConvergidas >= matrizFeromonas.length*0.85;

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
    private static class Solucion {
        private final List<Ruta> rutas;
        private final double fitness;
        private final int[] asignaciones;

        public Solucion(List<Ruta> rutas, double fitness, int[] asignaciones) {
            this.rutas = rutas;
            this.fitness = fitness;
            this.asignaciones = asignaciones;
        }
    }

    /**
     * Simula la posibilidad de avería en un camión durante la simulación
     * @param camion Camión a evaluar
     * @param distancia Distancia que recorrería
     * @return true si hay avería, false si no
     */
    private boolean simularAveria(Camion camion, double distancia) {
        Random random = new Random();

        // Probabilidad base de avería por km
        double probBase = 0.0001; // 0.01% por km

        // Aumentar probabilidad según distancia total
        double probabilidadAveria = probBase * distancia;

        // Ajustar según tiempo desde último mantenimiento
        if (camion.getFechaUltimoMantenimiento() != null) {
            long diasDesdeMantenimiento = Duration.between(
                    camion.getFechaUltimoMantenimiento(), momentoActual).toDays();

            // Aumentar probabilidad si han pasado más de 30 días
            if (diasDesdeMantenimiento > 30) {
                probabilidadAveria *= (1 + (diasDesdeMantenimiento - 30) * 0.03);
            }
        }

        // Limitar probabilidad máxima a 30%
        if (probabilidadAveria > 0.3) {
            probabilidadAveria = 0.3;
        }

        // Determinar si hay avería
        return random.nextDouble() < probabilidadAveria;
    }

    /**
     * Determina el tipo de avería aleatoriamente
     * @return Tipo de incidente
     */
    private TipoIncidente determinarTipoAveria() {
        Random random = new Random();
        double valor = random.nextDouble();

        if (valor < 0.5) {
            return TipoIncidente.TI1; // 50% probabilidad TI1 (menos severo)
        } else if (valor < 0.8) {
            return TipoIncidente.TI2; // 30% probabilidad TI2
        } else {
            return TipoIncidente.TI3; // 20% probabilidad TI3 (más severo)
        }
    }
}