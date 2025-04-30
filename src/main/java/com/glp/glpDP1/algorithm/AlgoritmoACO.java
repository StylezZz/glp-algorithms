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

    /**
     * Constructor con parámetros predeterminados
     */
    public AlgoritmoACO() {
        this(30, 100, 1.0, 2.0, 0.1, 0.9, 0.1);
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
            List<Solucion> soluciones = new ArrayList<>();

            // Cada hormiga construye una solución
            for (int hormiga = 0; hormiga < numHormigas; hormiga++) {
                Solucion solucion = construirSolucion();
                soluciones.add(solucion);

                // Actualizar mejor solución global si es necesario
                if (solucion.fitness < mejorFitness) {
                    mejorSolucion = solucion.rutas;
                    mejorFitness = solucion.fitness;
                    System.out.println("Iter " + iteracion + ", Hormiga " + hormiga + ": Nuevo mejor fitness = " + mejorFitness);
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
        List<Pedido> pedidosProcesados = new ArrayList<>();
        for (Pedido pedido : pedidosOriginales) {
            if (pedido.getCantidadGLP() <= 25.0) {
                pedidosProcesados.add(pedido);
                continue;
            }

            double cantidadRestante = pedido.getCantidadGLP();
            int contador = 1;
            while (cantidadRestante > 0) {
                double cantidadParte = Math.min(cantidadRestante, 25.0);
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
            for (int j = 0; j <= numCamiones; j++) {
                matrizFeromonas[i][j] = inicialFeromona;
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

                // Si no cumple con requisitos básicos, la heurística es muy baja
                if (!capacidadSuficiente || !combustibleSuficiente) {
                    matrizHeuristica[i][j] = 0.01;
                } else {
                    // Inversamente proporcional al consumo (menor consumo = mejor)
                    matrizHeuristica[i][j] = 1.0 / (consumoEstimado + 0.1);
                }
            }

            // Opción de no asignar (última columna)
            matrizHeuristica[i][numCamiones] = 0.01; // Desfavorecemos no asignar
        }
    }

    /**
     * Construye una solución completa (una hormiga)
     */
    private Solucion construirSolucion() {
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
        asignarPedidosIniciales(asignaciones, pedidosNoAsignados);

        // Para cada pedido no asignado, decidir a qué camión asignarlo
        while (!pedidosNoAsignados.isEmpty()) {
            // Elegir un pedido aleatoriamente
            int indexPedido = pedidosNoAsignados.remove(new Random().nextInt(pedidosNoAsignados.size()));

            // Decidir a qué camión asignarlo
            int camionElegido = elegirCamion(indexPedido, asignaciones);

            // Asignar pedido
            asignaciones[indexPedido] = camionElegido;
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

        // Shufflear camiones para asignación aleatoria
        List<Integer> indicesCamiones = new ArrayList<>();
        for (int i = 0; i < camionesDisponibles.size(); i++) {
            indicesCamiones.add(i);
        }
        Collections.shuffle(indicesCamiones);

        // Para cada camión, intentar asignar el pedido más adecuado
        for (int indiceCamion : indicesCamiones) {
            // Si ya no hay pedidos, terminar
            if (pedidosNoAsignados.isEmpty()) break;

            Camion camion = camionesDisponibles.get(indiceCamion);
            int mejorPedido = -1;
            double mejorValor = -1;

            // Buscar el pedido más adecuado para este camión
            for (int indicePedido : pedidosNoAsignados) {
                Pedido pedido = pedidosPendientes.get(indicePedido);

                // Verificar si el camión puede manejar este pedido
                if (camion.getCapacidadTanqueGLP() < pedido.getCantidadGLP()) continue;

                // Verificar distancia y combustible
                int distancia = camion.getUbicacionActual().distanciaA(pedido.getUbicacion());
                if (camion.calcularDistanciaMaxima() < distancia) continue;

                // Calcular valor de asignación (combinación de feromona y heurística)
                double valor = matrizFeromonas[indicePedido][indiceCamion] *
                        Math.pow(matrizHeuristica[indicePedido][indiceCamion], beta);

                if (valor > mejorValor) {
                    mejorValor = valor;
                    mejorPedido = indicePedido;
                }
            }

            // Si encontramos un pedido adecuado, asignarlo
            if (mejorPedido != -1) {
                asignaciones[mejorPedido] = indiceCamion;
                pedidosNoAsignados.remove(Integer.valueOf(mejorPedido));
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

        // Optimizar el orden de cada ruta
        for (Ruta ruta : rutasPorCamion.values()) {
            ruta.optimizarSecuencia();
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

        // Conjunto de pedidos asignados
        Set<String> pedidosAsignadosIds = new HashSet<>();

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
                pedidosAsignadosIds.add(p.getId());
            }

            if (glpTotal > camion.getCapacidadTanqueGLP()) {
                sobrecargaTotal += (glpTotal - camion.getCapacidadTanqueGLP());
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

            // Penalización por averías (probabilidad proporcional a distancia)
            // Esto es una heurística simple: más distancia = más probabilidad de avería
            if (ruta.getDistanciaTotal() > 50) { // Si la ruta es larga
                penalizacionAverias += ruta.getDistanciaTotal() * 0.01; // 1% por km
            }
        }

        // Actualizar pedidos no asignados
        pedidosNoAsignados -= pedidosAsignadosIds.size();

        // Calcular fitness final (ponderado, con mayor énfasis en consumo)
        double fitness = 0.25 * consumoTotal +
                0.10 * distanciaTotal +
                0.15 * retrasosTotal +
                0.1 * (pedidosNoAsignados * 1000) +  // Penalización fuerte por pedidos no asignados
                0.10 * (sobrecargaTotal * 1000) +     // Penalización fuerte por sobrecarga
                0.05 * penalizacionAverias;           // Penalización por probabilidad de averías

        return fitness;
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
    }

    /**
     * Verifica si el algoritmo ha convergido
     */
    private boolean haConvergido() {
        // Comprobar si la matriz de feromonas ha convergido
        // (cuando la mayoría de las filas tienen un valor dominante)
        int filasConvergidas = 0;
        double umbralConvergencia = 0.9; // El valor dominante debe ser al menos umbral veces mayor que los demás

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

            if (max > segundoMax * umbralConvergencia) {
                filasConvergidas++;
            }
        }

        // Si la mayoría de filas han convergido, considerar el algoritmo convergido
        return filasConvergidas >= matrizFeromonas.length * 0.7;
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