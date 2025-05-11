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

    // Resultados
    private List<Ruta> mejorSolucion;
    private double mejorFitness;

    // Matriz de feromonas y heurísticas
    private List<Ubicacion> ubicacionesRelevantes;
    private Map<Ubicacion, List<Pedido>> pedidosPorUbicacion;

    private double feromonaMinima;
    private double feromonaMaxima;

    private List<Double> historialFitness = new ArrayList<>();


    // Reemplazar la estructura de feromonas actual en AlgoritmoACO.java (líneas ~208-213)
    private Map<UbicacionTemporal, Map<UbicacionTemporal, Double>> matrizFeromonas;
    private Map<Ubicacion, Map<Ubicacion, Double>> matrizHeuristica;

    // Clase auxiliar para manejar ubicaciones con información temporal
    private static class UbicacionTemporal {
        private final Ubicacion ubicacion;
        private final int intervaloTiempo; // Intervalos de 15 minutos (0-95 para un día)

        public UbicacionTemporal(Ubicacion ubicacion, LocalDateTime tiempo) {
            this.ubicacion = ubicacion;
            // Discretizar en intervalos de 15 minutos (más granular que por hora)
            this.intervaloTiempo = tiempo.getHour() * 4 + tiempo.getMinute() / 15;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UbicacionTemporal that = (UbicacionTemporal) o;
            return intervaloTiempo == that.intervaloTiempo &&
                    Objects.equals(ubicacion, that.ubicacion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ubicacion, intervaloTiempo);
        }
    }

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
     * @param camiones      Lista de camiones disponibles
     * @param pedidos       Lista de pedidos pendientes
     * @param mapa          Mapa de la ciudad
     * @param momentoActual Momento actual para la planificación
     * @return Lista de rutas optimizadas
     */
    // Reemplazar el método optimizarRutas
    public List<Ruta> optimizarRutas(List<Camion> camiones, List<Pedido> pedidos,
                                     Mapa mapa, LocalDateTime momentoActual) {
        this.camionesDisponibles = filtrarCamionesDisponibles(camiones, momentoActual);
        this.pedidosPendientes = preprocesarPedidos(pedidos);
        this.mapa = mapa;
        this.iteracionActual = 0;

        LocalDate fechaActual = momentoActual.toLocalDate();
        mapa.filtrarBloqueosParaFecha(fechaActual);

        // Si no hay camiones disponibles o pedidos pendientes, retornar lista vacía
        if (camionesDisponibles.isEmpty() || pedidosPendientes.isEmpty()) {
            return new ArrayList<>();
        }

        // Inicializar matrices de feromonas y heurística
        // System.out.println("Inicializando matrices de feromonas y heurística...");
        inicializarMatrices(momentoActual);

        // Inicializar la mejor solución conocida
        mejorSolucion = null;
        mejorFitness = Double.MAX_VALUE;

        // Ciclo principal del algoritmo ACO mejorado (IACO)
        for (int iteracion = 0; iteracion < numIteraciones; iteracion++) {
            this.iteracionActual = iteracion;
            List<Solucion> soluciones = new ArrayList<>();

            // Cada hormiga construye una solución
            for (int hormiga = 0; hormiga < numHormigas; hormiga++) {
                System.out.println("Construyendo solución para hormiga " + hormiga + " en iteración " + iteracion);
                double q0Actual = calcularQ0Adaptativo(iteracion);
                Solucion solucion = construirSolucion(q0Actual, momentoActual);

                // MEJORA IACO: Aplicar mutación
                double probMutacion = calcularProbabilidadMutacion(iteracion);
                aplicarMutacion(solucion.rutas, probMutacion);

                // MEJORA IACO: Aplicar búsqueda local
                aplicarBusquedaLocal(solucion.rutas, momentoActual);

                // Recalcular fitness después de las mejoras
                double nuevoFitness = calcularFitness(solucion.rutas, momentoActual);
                soluciones.add(new Solucion(solucion.rutas, nuevoFitness));

                // Actualizar mejor solución global si es necesario
                if (nuevoFitness < mejorFitness) {
                    mejorSolucion = solucion.rutas;
                    mejorFitness = nuevoFitness;
                    System.out.println("Iter " + iteracion + ", Hormiga " + hormiga +
                            ": Nuevo mejor fitness = " + mejorFitness);
                    historialFitness.add(mejorFitness);
                }
            }

            // Actualizar feromonas con método mejorado
            actualizarFeromonas(soluciones, momentoActual);

            // Verificar convergencia
            if (iteracion > 20 && haConvergido(momentoActual)) {
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
     * Verifica si un camión tiene mantenimiento programado pronto
     */
    private boolean estaEnMantenimientoProgramado(Camion camion, LocalDateTime momentoActual) {
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
    private void inicializarMatrices(LocalDateTime momentoActual) {
        // Lista para almacenar las ubicaciones relevantes
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

        // Agrupar pedidos por ubicación
        pedidosPorUbicacion = new HashMap<>();
        if (pedidosPendientes != null && !pedidosPendientes.isEmpty()) {
            for (Pedido pedido : pedidosPendientes) {
                pedidosPorUbicacion.computeIfAbsent(pedido.getUbicacion(), k -> new ArrayList<>())
                        .add(pedido);
            }
        }

        // Inicializar matrices con la nueva estructura temporal
        matrizFeromonas = new HashMap<>();
        matrizHeuristica = new HashMap<>();

        // Crear matriz de heurística (no cambia con el tiempo)
        for (Ubicacion origen : ubicacionesRelevantes) {
            Map<Ubicacion, Double> heuristicaDesde = new HashMap<>();

            for (Ubicacion destino : ubicacionesRelevantes) {
                // Para heurística, si origen == destino, usar un valor alto para evitarlo
                double distancia = origen.distanciaA(destino);
                double valorHeuristica = distancia == 0 ? 0.0 : 1.0 / (distancia + 0.1);
                heuristicaDesde.put(destino, valorHeuristica);
            }

            matrizHeuristica.put(origen, heuristicaDesde);
        }

        // Inicializar matriz de feromonas con componente temporal
        // Usaremos intervalos de 15 minutos por un día completo (96 intervalos)
        for (Ubicacion origen : ubicacionesRelevantes) {
            for (Ubicacion destino : ubicacionesRelevantes) {
                // Para cada par de ubicaciones, inicializar feromonas para diferentes intervalos temporales
                for (int hora = 0; hora < 24; hora++) {
                    for (int intervalo = 0; intervalo < 4; intervalo++) {
                        LocalDateTime tiempo = momentoActual.withHour(hora).withMinute(intervalo * 15);
                        UbicacionTemporal origenTemp = new UbicacionTemporal(origen, tiempo);
                        UbicacionTemporal destinoTemp = new UbicacionTemporal(destino, tiempo);

                        // No permitir auto-lazos (origen == destino)
                        double valorInicial = origen.equals(destino) ? 0.0 : inicialFeromona;

                        // Verificar si hay bloqueos conocidos en este momento
                        if (!origen.equals(destino)) {
                            if (mapa.estaBloqueado(destino, tiempo) ||
                                    mapa.tramoBloqueado(origen, destino, tiempo)) {
                                valorInicial *= 0.1; // Reducir feromona inicial para tramos bloqueados
                            }
                        }

                        matrizFeromonas.computeIfAbsent(origenTemp, k -> new HashMap<>())
                                .put(destinoTemp, valorInicial);
                    }
                }
            }
        }

        // Inicializar límites MMAS
        feromonaMaxima = 1.0 / (rho * inicialFeromona);
        feromonaMinima = feromonaMaxima * 0.1;
    }

    /**
     * Construye una solución completa (una hormiga)
     */
    private Solucion construirSolucion(double q0Actual, LocalDateTime momentoActual) {
        List<Ruta> rutas = new ArrayList<>();
        Set<Pedido> pedidosEntregados = new HashSet<>();

        // Para cada camión construir su ruta completa
        for (Camion camion : camionesDisponibles) {
            Ruta ruta = construirRutaParaCamion(camion, pedidosEntregados, q0Actual, momentoActual);
            if (!ruta.getPedidosAsignados().isEmpty()) {
                rutas.add(ruta);
            }
        }

        // Calcular fitness
        double fitness = calcularFitness(rutas, momentoActual);

        return new Solucion(rutas, fitness);
    }

    // Añadir después del método construirSolucion (aprox. línea 550)
    private double calcularProbabilidadMutacion(int iteracion) {
        // Implementación de la Ecuación (13) del paper
        double pmin = 1.0 / pedidosPendientes.size(); // Probabilidad mínima
        double pmax = 1.0 / camionesDisponibles.size(); // Probabilidad máxima

        // La probabilidad disminuye con las iteraciones
        return pmin + (pmax - pmin) * Math.pow(1 - ((double) iteracion / numIteraciones), 2);
    }

    private void aplicarMutacion(List<Ruta> rutas, double probabilidadMutacion) {
        if (rutas.size() < 2) return; // Necesitamos al menos 2 rutas para intercambiar

        // Generar valores aleatorios para cada nodo en cada ruta
        Map<Ruta, Map<Ubicacion, Double>> valoresAleatorios = new HashMap<>();

        for (Ruta ruta : rutas) {
            Map<Ubicacion, Double> valoresRuta = new HashMap<>();
            for (Ubicacion nodo : ruta.getSecuenciaNodos()) {
                valoresRuta.put(nodo, Math.random());
            }
            valoresAleatorios.put(ruta, valoresRuta);
        }

        // Encontrar rutas con nodos candidatos para mutación
        List<Ruta> rutasConCandidatos = new ArrayList<>();
        for (Ruta ruta : rutas) {
            boolean tieneCandidato = false;
            for (Ubicacion nodo : ruta.getSecuenciaNodos()) {
                if (valoresAleatorios.get(ruta).get(nodo) <= probabilidadMutacion) {
                    tieneCandidato = true;
                    break;
                }
            }
            if (tieneCandidato) {
                rutasConCandidatos.add(ruta);
            }
        }

        // Si tenemos menos de 2 rutas con candidatos, no podemos hacer mutación
        if (rutasConCandidatos.size() < 2) return;

        // Seleccionar 2 rutas aleatoriamente entre las que tienen candidatos
        Collections.shuffle(rutasConCandidatos);
        Ruta ruta1 = rutasConCandidatos.get(0);
        Ruta ruta2 = rutasConCandidatos.get(1);

        // Encontrar nodos candidatos en cada ruta
        List<Ubicacion> candidatos1 = new ArrayList<>();
        List<Ubicacion> candidatos2 = new ArrayList<>();

        for (Ubicacion nodo : ruta1.getSecuenciaNodos()) {
            if (valoresAleatorios.get(ruta1).get(nodo) <= probabilidadMutacion) {
                candidatos1.add(nodo);
            }
        }

        for (Ubicacion nodo : ruta2.getSecuenciaNodos()) {
            if (valoresAleatorios.get(ruta2).get(nodo) <= probabilidadMutacion) {
                candidatos2.add(nodo);
            }
        }

        // Si alguna ruta no tiene candidatos, no hacemos nada
        if (candidatos1.isEmpty() || candidatos2.isEmpty()) return;

        // Seleccionar un nodo aleatorio de cada lista
        Ubicacion nodo1 = candidatos1.get(new Random().nextInt(candidatos1.size()));
        Ubicacion nodo2 = candidatos2.get(new Random().nextInt(candidatos2.size()));

        // Intercambiar nodos entre rutas si es factible
        if (esMutacionFactible(ruta1, ruta2, nodo1, nodo2)) {
            intercambiarNodos(ruta1, ruta2, nodo1, nodo2);
        }
    }

    private boolean esMutacionFactible(Ruta ruta1, Ruta ruta2, Ubicacion nodo1, Ubicacion nodo2) {
        // Verificar si el intercambio respeta las restricciones:
        // - Capacidad de los camiones
        // - No viola bloqueos temporales
        // - No afecta el mantenimiento programado

        // Esta es una implementación simplificada. En un caso real,
        // habría que verificar más condiciones.

        // Obtener los camiones asociados a cada ruta
        Camion camion1 = getCamionPorCodigo(ruta1.getCodigoCamion());
        Camion camion2 = getCamionPorCodigo(ruta2.getCodigoCamion());

        if (camion1 == null || camion2 == null) return false;

        // Verificar si los nodos tienen pedidos asociados
        List<Pedido> pedidosNodo1 = obtenerPedidosEnUbicacion(nodo1, ruta1.getPedidosAsignados());
        List<Pedido> pedidosNodo2 = obtenerPedidosEnUbicacion(nodo2, ruta2.getPedidosAsignados());

        double glpTotalNodo1 = pedidosNodo1.stream().mapToDouble(Pedido::getCantidadGLP).sum();
        double glpTotalNodo2 = pedidosNodo2.stream().mapToDouble(Pedido::getCantidadGLP).sum();

        // Calcular nuevo GLP total para cada camión después del intercambio
        double nuevoGlpCamion1 = calcularGlpTotalRuta(ruta1) - glpTotalNodo1 + glpTotalNodo2;
        double nuevoGlpCamion2 = calcularGlpTotalRuta(ruta2) - glpTotalNodo2 + glpTotalNodo1;

        // Verificar capacidad
        if (nuevoGlpCamion1 > camion1.getCapacidadTanqueGLP() ||
                nuevoGlpCamion2 > camion2.getCapacidadTanqueGLP()) {
            return false;
        }

        // Aquí se añadirían más verificaciones según las restricciones del problema

        return true;
    }

    private void intercambiarNodos(Ruta ruta1, Ruta ruta2, Ubicacion nodo1, Ubicacion nodo2) {
        // Realizar el intercambio físico de nodos entre rutas
        List<Ubicacion> secuencia1 = new ArrayList<>(ruta1.getSecuenciaNodos());
        List<Ubicacion> secuencia2 = new ArrayList<>(ruta2.getSecuenciaNodos());

        int indice1 = secuencia1.indexOf(nodo1);
        int indice2 = secuencia2.indexOf(nodo2);

        if (indice1 == -1 || indice2 == -1) return;

        secuencia1.set(indice1, nodo2);
        secuencia2.set(indice2, nodo1);

        // Intercambiar pedidos asociados
        List<Pedido> pedidosNodo1 = obtenerPedidosEnUbicacion(nodo1, ruta1.getPedidosAsignados());
        List<Pedido> pedidosNodo2 = obtenerPedidosEnUbicacion(nodo2, ruta2.getPedidosAsignados());

        // Eliminar y agregar pedidos
        for (Pedido pedido : pedidosNodo1) {
            ruta1.eliminarPedido(pedido.getId());
            ruta2.agregarPedido(pedido);
        }

        for (Pedido pedido : pedidosNodo2) {
            ruta2.eliminarPedido(pedido.getId());
            ruta1.agregarPedido(pedido);
        }

        // Actualizar secuencias de nodos
        ruta1.setSecuenciaNodos(secuencia1);
        ruta2.setSecuenciaNodos(secuencia2);
    }

    private List<Pedido> obtenerPedidosEnUbicacion(Ubicacion ubicacion, List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> p.getUbicacion().equals(ubicacion))
                .collect(Collectors.toList());
    }

    private double calcularGlpTotalRuta(Ruta ruta) {
        return ruta.getPedidosAsignados().stream()
                .mapToDouble(Pedido::getCantidadGLP)
                .sum();
    }

    // Añadir después de los métodos de mutación
    private void aplicarBusquedaLocal(List<Ruta> rutas, LocalDateTime momentoActual) {
        for (Ruta ruta : rutas) {
            // Verificar si la ruta tiene violaciones de tiempo
            if (tieneViolacionesTiempo(ruta, momentoActual)) {
                optimizarRutaTiempo(ruta, momentoActual);
            }
        }
    }

    private boolean tieneViolacionesTiempo(Ruta ruta, LocalDateTime momentoActual) {
        // Simular la ejecución de la ruta para detectar violaciones de tiempo
        Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
        if (camion == null) return false;

        LocalDateTime tiempoActual = momentoActual;
        Ubicacion ubicacionActual = camion.getUbicacionActual();

        for (Ubicacion siguiente : ruta.getSecuenciaNodos()) {
            // Calcular tiempo de llegada al siguiente nodo
            int distancia = ubicacionActual.distanciaA(siguiente);
            double horasViaje = distancia / camion.getVelocidadPromedio();
            LocalDateTime tiempoLlegada = tiempoActual.plusMinutes((long) (horasViaje * 60));

            // Verificar si hay bloqueo en el momento de llegada
            if (mapa.estaBloqueado(siguiente, tiempoLlegada) ||
                    mapa.tramoBloqueado(ubicacionActual, siguiente, tiempoLlegada)) {
                return true;
            }

            // Verificar tiempo límite de entrega para pedidos en esta ubicación
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (pedido.getUbicacion().equals(siguiente) &&
                        tiempoLlegada.isAfter(pedido.getHoraLimiteEntrega())) {
                    return true;
                }
            }

            // Actualizar para próxima iteración
            tiempoActual = tiempoLlegada;
            ubicacionActual = siguiente;
        }

        return false;
    }

    private void optimizarRutaTiempo(Ruta ruta, LocalDateTime momentoActual) {
        List<Ubicacion> secuencia = ruta.getSecuenciaNodos();
        if (secuencia.size() <= 2) return; // No hay suficientes nodos para optimizar

        boolean mejora = true;
        while (mejora) {
            mejora = false;

            // Probar todos los posibles intercambios de nodos internos
            for (int i = 1; i < secuencia.size() - 1; i++) {
                for (int j = i + 1; j < secuencia.size() - 1; j++) {
                    // Crear una copia de la secuencia para probar el intercambio
                    List<Ubicacion> nuevaSecuencia = new ArrayList<>(secuencia);

                    // Intercambiar nodos
                    Ubicacion temp = nuevaSecuencia.get(i);
                    nuevaSecuencia.set(i, nuevaSecuencia.get(j));
                    nuevaSecuencia.set(j, temp);

                    // Verificar si el intercambio reduce las violaciones
                    if (evaluarMejoraTiempo(ruta, secuencia, nuevaSecuencia, momentoActual)) {
                        secuencia = nuevaSecuencia;
                        ruta.setSecuenciaNodos(nuevaSecuencia);
                        mejora = true;
                        break;
                    }
                }
                if (mejora) break;
            }
        }
    }

    private boolean evaluarMejoraTiempo(Ruta ruta, List<Ubicacion> secuenciaOriginal,
                                        List<Ubicacion> nuevaSecuencia, LocalDateTime momentoActual) {
        // Simular ambas secuencias y comparar violaciones
        int violacionesOriginal = contarViolacionesTiempo(ruta, secuenciaOriginal, momentoActual);
        int violacionesNueva = contarViolacionesTiempo(ruta, nuevaSecuencia, momentoActual);

        return violacionesNueva < violacionesOriginal;
    }

    private int contarViolacionesTiempo(Ruta ruta, List<Ubicacion> secuencia, LocalDateTime momentoActual) {
        int violaciones = 0;
        Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
        if (camion == null) return Integer.MAX_VALUE;

        LocalDateTime tiempoActual = momentoActual;
        Ubicacion ubicacionActual = camion.getUbicacionActual();

        for (Ubicacion siguiente : secuencia) {
            // Calcular tiempo de llegada
            int distancia = ubicacionActual.distanciaA(siguiente);
            double horasViaje = distancia / camion.getVelocidadPromedio();
            LocalDateTime tiempoLlegada = tiempoActual.plusMinutes((long) (horasViaje * 60));

            // Contar violaciones
            if (mapa.estaBloqueado(siguiente, tiempoLlegada) ||
                    mapa.tramoBloqueado(ubicacionActual, siguiente, tiempoLlegada)) {
                violaciones++;
            }

            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (pedido.getUbicacion().equals(siguiente) &&
                        tiempoLlegada.isAfter(pedido.getHoraLimiteEntrega())) {
                    violaciones++;
                }
            }

            // Actualizar para próxima iteración
            tiempoActual = tiempoLlegada.plusMinutes(15); // 15 min para descarga
            ubicacionActual = siguiente;
        }

        return violaciones;
    }

    /**
     * Construye una ruta completa para un camión específico
     */
    private Ruta construirRutaParaCamion(Camion camion, Set<Pedido> pedidosEntregados, double q0, LocalDateTime momentoActual) {
        Ruta ruta = new Ruta(camion.getCodigo(), camion.getUbicacionActual());
        ruta.setDestino(mapa.obtenerAlmacenCentral().getUbicacion());

        Ubicacion ubicacionActual = camion.getUbicacionActual();
        LocalDateTime tiempoActual = momentoActual; // Asegúrate que momentoActual no sea null aquí
        if (tiempoActual == null) {
            tiempoActual = LocalDateTime.now(); // Valor de respaldo
            System.out.println("momentoActual es null en construirRutaParaCamion para camión {}" + camion.getCodigo());
        }
        double combustibleActual = camion.getNivelCombustibleActual();
        double glpActual = 0; // Camión vacío al inicio

        // Gestores para verificar durante la ruta
        GestorMantenimiento gestorMantenimiento = new GestorMantenimiento();
        GestorAverias gestorAverias = new GestorAverias();

        // Verificar si el camión entrará en mantenimiento durante la ruta
        LocalDateTime proximoMantenimiento = camion.getFechaProximoMantenimiento();
        boolean mantenimientoDuranteRuta = false;

        // Verificar si el camión tiene avería programada
        boolean averiaAplicada = false;
        GestorAverias.RegistroAveria averiaProgramada =
                gestorAverias.obtenerAveriaProgramada(camion.getCodigo(), tiempoActual);
        boolean tieneAveriaProgramada = (averiaProgramada != null);

        // Lista que mantiene toda la ruta planificada (para determinar punto de avería)
        List<Ubicacion> rutaCompleta = new ArrayList<>();
        rutaCompleta.add(ubicacionActual);

        // Seguir construyendo la ruta hasta que no sea factible continuar
        while (true) {
            // VERIFICACIÓN DE MANTENIMIENTO DURANTE RUTA
            if (proximoMantenimiento != null && tiempoActual.isAfter(proximoMantenimiento)) {
                // El camión ha entrado en periodo de mantenimiento durante la ruta
                System.out.println("¡Camión " + camion.getCodigo() +
                        " ha entrado en mantenimiento durante la ruta!");
                mantenimientoDuranteRuta = true;

                // Forzar regreso al almacén central
                Ubicacion ubicacionAlmacenCentral = mapa.obtenerAlmacenCentral().getUbicacion();

                // Solo si no estamos ya en el almacén central
                if (!ubicacionActual.equals(ubicacionAlmacenCentral)) {
                    List<Ubicacion> rutaRegreso = mapa.encontrarRutaConTiempo(
                            ubicacionActual,
                            ubicacionAlmacenCentral,
                            tiempoActual,
                            camion.getVelocidadPromedio()
                    );

                    if (!rutaRegreso.isEmpty()) {
                        // Añadir regreso a la ruta
                        for (Ubicacion nodo : rutaRegreso) {
                            if (!ruta.getSecuenciaNodos().contains(nodo)) {
                                ruta.getSecuenciaNodos().add(nodo);
                            }
                        }

                        // Registrar evento de interrupción
                        registrarEvento(ruta, EventoRuta.TipoEvento.MANTENIMIENTO,
                                tiempoActual, ubicacionAlmacenCentral);
                    }
                }

                // Terminar la construcción de la ruta
                break;
            }

            // VERIFICACIÓN DE AVERÍA
            // Si tenemos avería programada y ya tenemos suficiente ruta construida
            if (tieneAveriaProgramada && !averiaAplicada && rutaCompleta.size() >= 4) {
                // Determinar si aplicamos la avería en este punto
                // La avería ocurre entre el 5% y 35% del recorrido total de la ruta
                int totalNodosEstimados = rutaCompleta.size() * 4; // Estimación del tamaño total
                int minNodo = Math.max(1, (int) (totalNodosEstimados * 0.05));
                int maxNodo = Math.min(rutaCompleta.size() - 1, (int) (totalNodosEstimados * 0.35));

                // Si estamos en el rango de puntos donde puede ocurrir la avería
                if (rutaCompleta.size() >= minNodo && rutaCompleta.size() <= maxNodo) {
                    // Probabilidad de que ocurra la avería en este nodo
                    // Mayor probabilidad en nodos intermedios del rango
                    double probabilidadAveria = 0.2; // 20% de probabilidad en cada nodo elegible

                    if (Math.random() < probabilidadAveria) {
                        // ¡Ocurre la avería!
                        System.out.println("¡Avería tipo " + averiaProgramada.getTipoIncidente() +
                                " del camión " + camion.getCodigo() + " en ubicación " + ubicacionActual);

                        // Registrar el evento de avería
                        registrarEventoAveria(ruta, averiaProgramada.getTipoIncidente(),
                                tiempoActual, ubicacionActual);

                        // Marcar la avería como aplicada
                        gestorAverias.marcarAveriaComoAplicada(averiaProgramada);
                        averiaAplicada = true;

                        // Calcular tiempo de inmovilización
                        LocalDateTime finInmovilizacion = gestorAverias.calcularFinInmovilizacion(
                                averiaProgramada.getTipoIncidente(), tiempoActual);

                        // Avanzar el tiempo hasta fin de inmovilización
                        tiempoActual = finInmovilizacion;

                        // Comportamiento según tipo de avería
                        if (averiaProgramada.getTipoIncidente().equals("TI1")) {
                            // TI1: Continuar la ruta después de la inmovilización
                            System.out.println("Camión " + camion.getCodigo() +
                                    " continúa la ruta después de avería TI1");
                            // No hacemos nada especial, continuamos normalmente

                        } else {
                            // TI2 o TI3: Regresar al almacén central
                            System.out.println("Camión " + camion.getCodigo() +
                                    " regresa al almacén central después de avería " +
                                    averiaProgramada.getTipoIncidente());

                            // Buscar ruta al almacén central
                            Ubicacion ubicacionAlmacenCentral = mapa.obtenerAlmacenCentral().getUbicacion();
                            List<Ubicacion> rutaRegreso = mapa.encontrarRutaConTiempo(
                                    ubicacionActual,
                                    ubicacionAlmacenCentral,
                                    tiempoActual,
                                    camion.getVelocidadPromedio());

                            if (!rutaRegreso.isEmpty()) {
                                // Añadir regreso a la secuencia de nodos
                                for (Ubicacion nodo : rutaRegreso) {
                                    if (!ruta.getSecuenciaNodos().contains(nodo)) {
                                        ruta.getSecuenciaNodos().add(nodo);
                                    }
                                }

                                // Registrar evento de fin por avería
                                ruta.registrarEvento(EventoRuta.TipoEvento.FIN_POR_AVERIA,
                                        tiempoActual.plusMinutes(rutaRegreso.size() * 2), // Estimación
                                        ubicacionAlmacenCentral,
                                        "Fin de ruta por avería tipo " + averiaProgramada.getTipoIncidente());

                                // Añadir pedidos no entregados a pendientes para reasignación
                                for (Pedido pedido : ruta.getPedidosAsignados()) {
                                    if (!pedido.isEntregado()) {
                                        pedidosEntregados.remove(pedido);
                                    }
                                }
                            }

                            // Terminar la construcción de la ruta
                            break;
                        }
                    }
                }
            }

            // Buscar próxima ubicación
            Ubicacion proximaUbicacion = elegirProximaUbicacion(
                    ubicacionActual,
                    tiempoActual,
                    pedidosEntregados,
                    q0,
                    camion,
                    glpActual,
                    combustibleActual
            );

            if (proximaUbicacion == null) break; // No hay más ubicaciones factibles

            // Calcular ruta A* a la próxima ubicación
            List<Ubicacion> rutaHastaProxima = mapa.encontrarRutaConTiempo(
                    ubicacionActual,
                    proximaUbicacion,
                    tiempoActual,
                    camion.getVelocidadPromedio()
            );

            if (rutaHastaProxima.isEmpty()) break; // No se puede llegar

            // Calcular distancia y consumo
            int distancia = 0;
            for (int i = 0; i < rutaHastaProxima.size() - 1; i++) {
                distancia += rutaHastaProxima.get(i).distanciaA(rutaHastaProxima.get(i + 1));
            }

            double consumo = camion.calcularConsumoCombustible(distancia);

            // Verificar si hay suficiente combustible
            if (combustibleActual < consumo) {
                // Necesita recarga, ir al almacén más cercano
                Almacen almacenCercano = mapa.obtenerAlmacenMasCercano(ubicacionActual);
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

            // Actualizar ubicación actual y añadir a la ruta completa
            ubicacionActual = proximaUbicacion;
            // Agregar la ubicación a la ruta completa para determinar punto de avería
            rutaCompleta.add(ubicacionActual);

            // Si ya se han entregado todos los pedidos factibles, terminar
            LocalDateTime finalTiempoActual = tiempoActual;
            if (pedidosPendientes.stream()
                    .filter(p -> !pedidosEntregados.contains(p))
                    .filter(p -> !p.estaRetrasado(finalTiempoActual))
                    .count() == 0) {
                break;
            }
        }

        // Si no ocurrió avería o mantenimiento, construir regreso al almacén central
        if (!averiaAplicada && !mantenimientoDuranteRuta) {
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

                // Añadir nodos a la secuencia
                for (Ubicacion nodo : rutaRegreso) {
                    if (!ruta.getSecuenciaNodos().contains(nodo)) {
                        ruta.getSecuenciaNodos().add(nodo);
                    }
                }

                registrarEvento(ruta, EventoRuta.TipoEvento.FIN,
                        tiempoActual, mapa.obtenerAlmacenCentral().getUbicacion());
            }
        }

        // Si entramos en mantenimiento o avería TI2/TI3 durante la ruta, marcar pedidos como no entregados
        if (mantenimientoDuranteRuta || (averiaAplicada &&
                !averiaProgramada.getTipoIncidente().equals("TI1"))) {
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (!pedido.isEntregado()) {
                    // Estos pedidos quedarán sin entregar y deberán ser reasignados
                    pedidosEntregados.remove(pedido);
                }
            }
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

        // Crear objeto temporal para la ubicación actual en el tiempo específico
        UbicacionTemporal actualTemp = new UbicacionTemporal(actual, tiempo);

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
                UbicacionTemporal destinoTemp = new UbicacionTemporal(destino, tiempo);

                // Obtener feromona considerando tiempo
                double feromona = matrizFeromonas.getOrDefault(actualTemp, Collections.emptyMap())
                        .getOrDefault(destinoTemp, inicialFeromona);

                // Obtener heurística (no dependiente del tiempo)
                double heuristica = matrizHeuristica.getOrDefault(actual, Collections.emptyMap())
                        .getOrDefault(destino, 0.1);

                // Factor de atracción adicional para pedidos urgentes
                double factorUrgencia = 1.0;
                List<Pedido> pedidosEnDestino = pedidosPorUbicacion.getOrDefault(destino, Collections.emptyList());
                for (Pedido pedido : pedidosEnDestino) {
                    if (!pedidosEntregados.contains(pedido)) {
                        // Calcular tiempo estimado de llegada
                        int distancia = actual.distanciaA(destino);
                        double horasViaje = distancia / camion.getVelocidadPromedio();
                        LocalDateTime tiempoLlegada = tiempo.plusMinutes((long) (horasViaje * 60));

                        // Si estamos cerca del tiempo límite, aumentar urgencia
                        Duration tiempoRestante = Duration.between(tiempoLlegada, pedido.getHoraLimiteEntrega());
                        if (tiempoRestante.toHours() < 3) {
                            factorUrgencia = 1.5; // Más atractivo para pedidos urgentes
                            break;
                        }
                    }
                }

                // Calcular valor total con fórmula del ACO considerando factor de urgencia
                double valor = Math.pow(feromona, alfa) * Math.pow(heuristica, beta) * factorUrgencia;

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
                UbicacionTemporal destinoTemp = new UbicacionTemporal(destino, tiempo);

                // Obtener feromona considerando tiempo
                double feromona = matrizFeromonas.getOrDefault(actualTemp, Collections.emptyMap())
                        .getOrDefault(destinoTemp, inicialFeromona);

                // Obtener heurística (no dependiente del tiempo)
                double heuristica = matrizHeuristica.getOrDefault(actual, Collections.emptyMap())
                        .getOrDefault(destino, 0.1);

                // Agregar factor de urgencia similar al de arriba
                double factorUrgencia = 1.0;
                List<Pedido> pedidosEnDestino = pedidosPorUbicacion.getOrDefault(destino, Collections.emptyList());
                for (Pedido pedido : pedidosEnDestino) {
                    if (!pedidosEntregados.contains(pedido)) {
                        // Calcular tiempo estimado de llegada
                        int distancia = actual.distanciaA(destino);
                        double horasViaje = distancia / camion.getVelocidadPromedio();
                        LocalDateTime tiempoLlegada = tiempo.plusMinutes((long) (horasViaje * 60));

                        // Si estamos cerca del tiempo límite, aumentar urgencia
                        Duration tiempoRestante = Duration.between(tiempoLlegada, pedido.getHoraLimiteEntrega());
                        if (tiempoRestante.toHours() < 3) {
                            factorUrgencia = 1.5;
                            break;
                        }
                    }
                }

                double valor = Math.pow(feromona, alfa) * Math.pow(heuristica, beta) * factorUrgencia;
                probabilidades.put(destino, valor);
                sumaTotal += valor;
            }

            // Normalizar probabilidades y seleccionar
            double r = Math.random() * sumaTotal;
            double acumulado = 0;

            for (Map.Entry<Ubicacion, Double> entry : probabilidades.entrySet()) {
                acumulado += entry.getValue();
                if (r <= acumulado) {
                    return entry.getKey();
                }
            }

            // Si por alguna razón no se seleccionó ninguno, devolver el primero
            return ubicacionesFactibles.get(0);
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

        // VERIFICACIÓN: Rechazar auto-lazos (mismo origen y destino)
        if (origen.equals(destino)) {
            return false;
        }

        // VERIFICACIÓN: Evitar ir de un almacén a otro innecesariamente
        boolean origenEsAlmacen = esPuntoAlmacen(origen);
        boolean destinoEsAlmacen = esPuntoAlmacen(destino);

        // Si estamos en un almacén y el destino es otro almacén...
        if (origenEsAlmacen && destinoEsAlmacen && !origen.equals(destino)) {
            // Solo permitir si REALMENTE necesitamos ir (combustible o GLP bajo)
            boolean combustibleBajo = combustibleActual < (camion.getCapacidadTanqueCombustible() * 0.3);
            boolean glpBajo = glpActual < (camion.getCapacidadTanqueGLP() * 0.3);

            // Si ni el combustible ni el GLP están bajos, no es factible ir a otro almacén
            if (!combustibleBajo && !glpBajo) {
                return false;
            }
        }

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

    // Método auxiliar para determinar si un punto es un almacén
    private boolean esPuntoAlmacen(Ubicacion ubicacion) {
        // Verificar si coincide con el almacén central
        if (ubicacion.equals(mapa.obtenerAlmacenCentral().getUbicacion())) {
            return true;
        }

        // Verificar si coincide con algún almacén intermedio
        for (Almacen almacen : mapa.obtenerAlmacenesIntermedios()) {
            if (ubicacion.equals(almacen.getUbicacion())) {
                return true;
            }
        }

        return false;
    }


    /**
     * Actualiza la matriz de feromonas basado en las soluciones
     */
    // Reemplazar el método actualizarFeromonas existente
    private void actualizarFeromonas(List<Solucion> soluciones, LocalDateTime momentoActual) {
        // Evaporación global
        for (Map.Entry<UbicacionTemporal, Map<UbicacionTemporal, Double>> entry :
                matrizFeromonas.entrySet()) {
            for (Map.Entry<UbicacionTemporal, Double> innerEntry : entry.getValue().entrySet()) {
                double valorActual = innerEntry.getValue();
                innerEntry.setValue(valorActual * (1 - rho));
            }
        }

        // Actualización local para cada solución
        for (Solucion solucion : soluciones) {
            double deltaLocal = 1.0 / (solucion.fitness + 0.1);

            for (Ruta ruta : solucion.rutas) {
                List<Ubicacion> nodos = ruta.getSecuenciaNodos();
                LocalDateTime tiempoRuta = momentoActual;

                for (int i = 0; i < nodos.size() - 1; i++) {
                    Ubicacion actual = nodos.get(i);
                    Ubicacion siguiente = nodos.get(i + 1);

                    // Crear objetos temporales
                    UbicacionTemporal actualTemp = new UbicacionTemporal(actual, tiempoRuta);

                    // Calcular tiempo de llegada al siguiente nodo
                    Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
                    int distancia = actual.distanciaA(siguiente);
                    double horasViaje = camion != null ?
                            distancia / camion.getVelocidadPromedio() : distancia / 50.0;
                    tiempoRuta = tiempoRuta.plusMinutes((long) (horasViaje * 60));

                    UbicacionTemporal siguienteTemp = new UbicacionTemporal(siguiente, tiempoRuta);

                    // Actualizar feromona - Ecuación (14) del paper
                    matrizFeromonas.computeIfAbsent(actualTemp, k -> new HashMap<>())
                            .compute(siguienteTemp, (k, v) -> (v == null ? inicialFeromona : v) + deltaLocal);
                }
            }
        }

        // Actualización global para la mejor solución - Ecuación (16) del paper
        if (mejorSolucion != null) {
            double deltaMejor = 1.0 / (mejorFitness + 0.1);

            for (Ruta ruta : mejorSolucion) {
                List<Ubicacion> nodos = ruta.getSecuenciaNodos();
                LocalDateTime tiempoRuta = momentoActual;

                for (int i = 0; i < nodos.size() - 1; i++) {
                    Ubicacion actual = nodos.get(i);
                    Ubicacion siguiente = nodos.get(i + 1);

                    // Crear objetos temporales
                    UbicacionTemporal actualTemp = new UbicacionTemporal(actual, tiempoRuta);

                    // Calcular tiempo de llegada
                    Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
                    int distancia = actual.distanciaA(siguiente);
                    double horasViaje = camion != null ?
                            distancia / camion.getVelocidadPromedio() : distancia / 50.0;
                    tiempoRuta = tiempoRuta.plusMinutes((long) (horasViaje * 60));

                    UbicacionTemporal siguienteTemp = new UbicacionTemporal(siguiente, tiempoRuta);

                    // Actualización global con mayor impacto
                    matrizFeromonas.computeIfAbsent(actualTemp, k -> new HashMap<>())
                            .compute(siguienteTemp, (k, v) ->
                                    (v == null ? inicialFeromona : v * (1 - rho)) + deltaMejor);
                }
            }
        }

        // Aplicar límites MMAS (min-max)
        for (Map.Entry<UbicacionTemporal, Map<UbicacionTemporal, Double>> entry :
                matrizFeromonas.entrySet()) {
            for (Map.Entry<UbicacionTemporal, Double> innerEntry : entry.getValue().entrySet()) {
                double valor = innerEntry.getValue();
                if (valor > feromonaMaxima) {
                    innerEntry.setValue(feromonaMaxima);
                } else if (valor < feromonaMinima) {
                    innerEntry.setValue(feromonaMinima);
                }
            }
        }
    }

    /**
     * Calcula el fitness de una solución (menor es mejor)
     */
    private double calcularFitness(List<Ruta> rutas, LocalDateTime momentoActual) {
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
    private boolean haConvergido(LocalDateTime momentoActual) {
        // Comprobar si la matriz de feromonas ha convergido
        int intervalosVerificados = 0;
        int intervalosConvergidos = 0;
        double umbralConvergencia = 0.95;

        // Seleccionar algunos intervalos de tiempo representativos para verificar
        for (int hora = 0; hora < 24; hora += 4) {
            LocalDateTime tiempo = momentoActual.withHour(hora).withMinute(0);

            for (Ubicacion origen : ubicacionesRelevantes) {
                UbicacionTemporal origenTemp = new UbicacionTemporal(origen, tiempo);
                Map<UbicacionTemporal, Double> feromonasDesde = matrizFeromonas.get(origenTemp);

                // Si no hay feromonas para este origen temporal, continuar
                if (feromonasDesde == null || feromonasDesde.isEmpty()) {
                    continue;
                }

                // Encontrar el destino con mayor feromona y el segundo mayor
                double maxFeromona = 0;
                double segundoMaxFeromona = 0;

                for (Map.Entry<UbicacionTemporal, Double> entry : feromonasDesde.entrySet()) {
                    double feromona = entry.getValue();
                    if (feromona > maxFeromona) {
                        segundoMaxFeromona = maxFeromona;
                        maxFeromona = feromona;
                    } else if (feromona > segundoMaxFeromona) {
                        segundoMaxFeromona = feromona;
                    }
                }

                intervalosVerificados++;

                // Verificar si hay una clara preferencia (convergencia)
                if (segundoMaxFeromona == 0 || maxFeromona > segundoMaxFeromona * umbralConvergencia) {
                    intervalosConvergidos++;
                }
            }
        }

        // Determinar si la proporción de intervalos convergidos es suficiente
        double proporcionConvergidos = (double) intervalosConvergidos / intervalosVerificados;
        boolean convergenciaFeromonas = proporcionConvergidos >= 0.85; // 85% de los intervalos convergen

        // Verificar estabilidad del fitness
        boolean estabilidadFitness = false;
        if (historialFitness.size() >= 10) {
            double ultimoFitness = historialFitness.get(historialFitness.size() - 1);
            double desviacionMax = 0.001 * ultimoFitness; // 0.1% de variación máxima

            estabilidadFitness = true;
            for (int i = historialFitness.size() - 10; i < historialFitness.size(); i++) {
                if (Math.abs(historialFitness.get(i) - ultimoFitness) > desviacionMax) {
                    estabilidadFitness = false;
                    break;
                }
            }
        }

        // Consideramos que el algoritmo ha convergido si ambas condiciones son verdaderas
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

    private class GestorMantenimiento {
        private Map<String, List<LocalDateTime>> mantenimientosProgramados;

        public GestorMantenimiento() {
            mantenimientosProgramados = new HashMap<>();
            cargarMantenimientosEnMemoria();
        }

        // Carga datos de mantenimiento en memoria
        private void cargarMantenimientosEnMemoria() {
            // Definir programación bimensual de mantenimientos
            // Formato: añadimos fechas para mantenimientos programados (aaaammdd)

            // Mantenimientos para abril-mayo 2025
            mantenimientosProgramados.put("TA01", Arrays.asList(
                    LocalDateTime.of(2025, 4, 1, 0, 0),
                    LocalDateTime.of(2025, 6, 1, 0, 0),
                    LocalDateTime.of(2025, 8, 1, 0, 0),
                    LocalDateTime.of(2025, 10, 1, 0, 0),
                    LocalDateTime.of(2025, 12, 1, 0, 0)
            ));

            mantenimientosProgramados.put("TA02", Arrays.asList(
                    LocalDateTime.of(2025, 5, 1, 0, 0),
                    LocalDateTime.of(2025, 7, 1, 0, 0),
                    LocalDateTime.of(2025, 9, 1, 0, 0),
                    LocalDateTime.of(2025, 11, 1, 0, 0)
            ));

            // TD (camiones más pequeños)
            for (int i = 1; i <= 10; i++) {
                String codigo = "TD" + String.format("%02d", i);
                List<LocalDateTime> fechas = new ArrayList<>();

                // Cada TD tiene su mantenimiento en una fecha diferente del calendario
                int diaBase = (i * 2) + 3; // para distribuir mantenimientos

                for (int mes = 4; mes <= 12; mes += 2) {
                    fechas.add(LocalDateTime.of(2025, mes, diaBase, 0, 0));
                }

                mantenimientosProgramados.put(codigo, fechas);
            }

            // TC (camiones medianos)
            for (int i = 1; i <= 4; i++) {
                String codigo = "TC" + String.format("%02d", i);
                List<LocalDateTime> fechas = new ArrayList<>();

                int diaBase = (i * 3) + 2; // diferente distribución

                for (int mes = 4; mes <= 12; mes += 2) {
                    fechas.add(LocalDateTime.of(2025, mes, diaBase, 0, 0));
                }

                mantenimientosProgramados.put(codigo, fechas);
            }

            // TB (camiones medianos-grandes)
            for (int i = 1; i <= 4; i++) {
                String codigo = "TB" + String.format("%02d", i);
                List<LocalDateTime> fechas = new ArrayList<>();

                int diaBase = (i * 3) + 4; // diferente distribución

                for (int mes = 4; mes <= 12; mes += 2) {
                    fechas.add(LocalDateTime.of(2025, mes, diaBase, 0, 0));
                }

                mantenimientosProgramados.put(codigo, fechas);
            }
        }

        /**
         * Verifica si un camión está en mantenimiento en un momento dado
         *
         * @param codigoCamion Código del camión
         * @param momento      Momento actual
         * @return true si está en mantenimiento, false en caso contrario
         */
        public boolean estaEnMantenimiento(String codigoCamion, LocalDateTime momento) {
            // Verificar que ni el código ni el momento sean nulos
            if (codigoCamion == null || momento == null || !mantenimientosProgramados.containsKey(codigoCamion)) {
                return false;
            }

            // Obtener lista de mantenimientos para este camión
            List<LocalDateTime> mantenimientos = mantenimientosProgramados.get(codigoCamion);

            // Verificar que la lista no sea nula
            if (mantenimientos == null) {
                return false;
            }

            for (LocalDateTime fechaMantenimiento : mantenimientos) {
                // Verificar que la fecha de mantenimiento no sea nula
                if (fechaMantenimiento == null) {
                    continue;
                }

                // El mantenimiento dura 24 horas, desde las 00:00 hasta las 23:59
                LocalDateTime finMantenimiento = fechaMantenimiento
                        .plusHours(23)
                        .plusMinutes(59)
                        .plusSeconds(59);

                if (momento.isEqual(fechaMantenimiento) ||
                        (momento.isAfter(fechaMantenimiento) && momento.isBefore(finMantenimiento)) ||
                        momento.isEqual(finMantenimiento)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Verifica si un camión tendrá mantenimiento en las próximas horas
         *
         * @param codigoCamion      Código del camión
         * @param momento           Momento actual
         * @param horasVerificacion Horas a verificar hacia adelante
         * @return true si tendrá mantenimiento, false en caso contrario
         */
        public boolean tendraMantenimientoProximamente(String codigoCamion,
                                                       LocalDateTime momento,
                                                       int horasVerificacion) {
            if (!mantenimientosProgramados.containsKey(codigoCamion)) {
                return false;
            }

            LocalDateTime limiteTiempo = momento.plusHours(horasVerificacion);

            for (LocalDateTime fechaMantenimiento : mantenimientosProgramados.get(codigoCamion)) {
                // Si el inicio del mantenimiento está dentro del periodo de verificación
                if ((fechaMantenimiento.isAfter(momento) || fechaMantenimiento.isEqual(momento)) &&
                        fechaMantenimiento.isBefore(limiteTiempo)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Encuentra la próxima fecha de mantenimiento para un camión
         * @param codigoCamion Código del camión
         * @param momento Momento actual
         * @return Próxima fecha de mantenimiento o null si no hay más mantenimientos programados
         */
        /**
         * Encuentra la próxima fecha de mantenimiento para un camión
         *
         * @param codigoCamion Código del camión
         * @param momento      Momento actual
         * @return Próxima fecha de mantenimiento o null si no hay más mantenimientos programados
         */
        public LocalDateTime proximoMantenimiento(String codigoCamion, LocalDateTime momento) {
            // Verificar que ni el código ni el momento sean nulos
            if (codigoCamion == null || momento == null || !mantenimientosProgramados.containsKey(codigoCamion)) {
                return null;
            }

            LocalDateTime proximaFecha = null;

            for (LocalDateTime fechaMantenimiento : mantenimientosProgramados.get(codigoCamion)) {
                // Verificar que la fecha de mantenimiento no sea nula
                if (fechaMantenimiento != null &&
                        fechaMantenimiento.isAfter(momento) &&
                        (proximaFecha == null || fechaMantenimiento.isBefore(proximaFecha))) {
                    proximaFecha = fechaMantenimiento;
                }
            }

            return proximaFecha;
        }
    }

    /**
     * Filtra los camiones disponibles considerando averías y mantenimientos
     */
    private List<Camion> filtrarCamionesDisponibles(List<Camion> camiones, LocalDateTime momentoActual) {
        GestorMantenimiento gestorMantenimiento = new GestorMantenimiento();
        GestorAverias gestorAverias = new GestorAverias();

        // Actualizar estados de camiones según mantenimiento y averías
        for (Camion camion : camiones) {
            // Actualizar según estado actual
            camion.actualizarEstado(momentoActual);

            // Actualizar según averías
            gestorAverias.actualizarEstadoCamionSegunAverias(camion, momentoActual);

            // Actualizar fecha de próximo mantenimiento en cada camión
            LocalDateTime proximoMantenimiento = gestorMantenimiento.proximoMantenimiento(
                    camion.getCodigo(), momentoActual);
            camion.setFechaProximoMantenimiento(proximoMantenimiento);
        }

        return camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE)
                .filter(c -> !gestorMantenimiento.estaEnMantenimiento(c.getCodigo(), momentoActual))
                .filter(c -> !debeEvitarsePorMantenimientoProximo(c, momentoActual))
                .filter(c -> !gestorAverias.tieneAveriaProgramada(c.getCodigo(), momentoActual))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un camión debe evitarse por mantenimiento próximo
     */
    private boolean debeEvitarsePorMantenimientoProximo(Camion camion, LocalDateTime momentoActual) {
        // Si tiene mantenimiento en las próximas 12 horas, no asignarlo a rutas nuevas
        if (camion.getFechaProximoMantenimiento() != null) {
            Duration hastaMantenimiento = Duration.between(
                    momentoActual, camion.getFechaProximoMantenimiento());
            return hastaMantenimiento.toHours() < 12;
        }
        return false;
    }

    /**
     * Clase para gestionar las averías de los camiones
     */
    private class GestorAverias {
        private Map<String, List<RegistroAveria>> averiasRegistradas;

        /**
         * Clase interna para almacenar los registros de averías
         */
        private static class RegistroAveria {
            private final String turno;
            private final String codigoCamion;
            private final String tipoIncidente;
            private boolean aplicada; // Para indicar si ya se aplicó la avería

            public RegistroAveria(String turno, String codigoCamion, String tipoIncidente) {
                this.turno = turno;
                this.codigoCamion = codigoCamion;
                this.tipoIncidente = tipoIncidente;
                this.aplicada = false;
            }

            public String getTurno() {
                return turno;
            }

            public String getCodigoCamion() {
                return codigoCamion;
            }

            public String getTipoIncidente() {
                return tipoIncidente;
            }

            public boolean isAplicada() {
                return aplicada;
            }

            public void setAplicada(boolean aplicada) {
                this.aplicada = aplicada;
            }

            @Override
            public String toString() {
                return turno + "_" + codigoCamion + "_" + tipoIncidente;
            }
        }

        public GestorAverias() {
            averiasRegistradas = new HashMap<>();
            cargarAveriasEnMemoria();
        }

        /**
         * Carga ejemplos de averías en memoria (simulando archivo)
         */
        private void cargarAveriasEnMemoria() {
            // Formato: turno_codigoCamion_tipoIncidente
            List<String> registrosAverias = Arrays.asList(
                    "T1_TA01_TI1", // Avería tipo 1 para TA01 en turno 1
                    "T2_TB02_TI2", // Avería tipo 2 para TB02 en turno 2
                    "T3_TC03_TI1", // Avería tipo 1 para TC03 en turno 3
                    "T1_TD04_TI3", // Avería tipo 3 para TD04 en turno 1
                    "T2_TA02_TI2", // Avería tipo 2 para TA02 en turno 2
                    "T3_TB01_TI1", // Avería tipo 1 para TB01 en turno 3
                    "T1_TC02_TI2", // Avería tipo 2 para TC02 en turno 1
                    "T2_TD05_TI1", // Avería tipo 1 para TD05 en turno 2
                    "T3_TA01_TI2", // Avería tipo 2 para TA01 en turno 3
                    "T1_TB03_TI3"  // Avería tipo 3 para TB03 en turno 1
            );

            // Procesar cada registro y almacenarlo
            for (String registro : registrosAverias) {
                String[] partes = registro.split("_");
                if (partes.length == 3) {
                    String turno = partes[0];
                    String codigoCamion = partes[1];
                    String tipoIncidente = partes[2];

                    RegistroAveria averia = new RegistroAveria(turno, codigoCamion, tipoIncidente);

                    // Agregar a la lista de averías para este camión
                    averiasRegistradas.computeIfAbsent(codigoCamion, k -> new ArrayList<>())
                            .add(averia);
                }
            }
        }

        /**
         * Determina el turno actual basado en la hora
         *
         * @param momento Hora actual
         * @return Código del turno (T1, T2 o T3)
         */
        public String obtenerTurnoActual(LocalDateTime momento) {
            int hora = momento.getHour();

            if (hora >= 0 && hora < 8) {
                return "T1";
            } else if (hora >= 8 && hora < 16) {
                return "T2";
            } else {
                return "T3";
            }
        }

        /**
         * Verifica si un camión tiene avería programada para el turno actual
         *
         * @param codigoCamion Código del camión
         * @param momento      Momento actual
         * @return true si tiene avería programada que no ha sido aplicada
         */
        public boolean tieneAveriaProgramada(String codigoCamion, LocalDateTime momento) {
            if (!averiasRegistradas.containsKey(codigoCamion)) {
                return false;
            }

            String turnoActual = obtenerTurnoActual(momento);

            for (RegistroAveria averia : averiasRegistradas.get(codigoCamion)) {
                if (averia.getTurno().equals(turnoActual) && !averia.isAplicada()) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Obtiene los detalles de la avería programada para un camión
         *
         * @param codigoCamion Código del camión
         * @param momento      Momento actual
         * @return La avería programada o null si no hay
         */
        public RegistroAveria obtenerAveriaProgramada(String codigoCamion, LocalDateTime momento) {
            if (!averiasRegistradas.containsKey(codigoCamion)) {
                return null;
            }

            String turnoActual = obtenerTurnoActual(momento);

            for (RegistroAveria averia : averiasRegistradas.get(codigoCamion)) {
                if (averia.getTurno().equals(turnoActual) && !averia.isAplicada()) {
                    return averia;
                }
            }

            return null;
        }

        /**
         * Calcula el momento en que termina la inmovilización de un camión
         *
         * @param tipoIncidente Tipo de incidente (TI1, TI2, TI3)
         * @param momentoAveria Momento en que ocurrió la avería
         * @return Momento en que termina la inmovilización
         */
        public LocalDateTime calcularFinInmovilizacion(String tipoIncidente, LocalDateTime momentoAveria) {
            switch (tipoIncidente) {
                case "TI1":
                case "TI2":
                    return momentoAveria.plusHours(2); // 2 horas inmovilizado
                case "TI3":
                    return momentoAveria.plusHours(4); // 4 horas inmovilizado
                default:
                    throw new IllegalArgumentException("Tipo de incidente no válido: " + tipoIncidente);
            }
        }

        /**
         * Calcula el momento en que el camión vuelve a estar disponible
         *
         * @param tipoIncidente Tipo de incidente (TI1, TI2, TI3)
         * @param momentoAveria Momento en que ocurrió la avería
         * @return Momento en que el camión vuelve a estar disponible
         */
        public LocalDateTime calcularFinIndisponibilidad(String tipoIncidente, LocalDateTime momentoAveria) {
            LocalDateTime finInmovilizacion = calcularFinInmovilizacion(tipoIncidente, momentoAveria);

            switch (tipoIncidente) {
                case "TI1":
                    // Inmediatamente después de la inmovilización
                    return finInmovilizacion;

                case "TI2":
                    // Depende del turno actual
                    String turnoAveria = obtenerTurnoActual(momentoAveria);
                    LocalDateTime hoy = momentoAveria.toLocalDate().atStartOfDay();

                    if (turnoAveria.equals("T1")) {
                        // Si ocurre en T1, disponible en T3 (desde las 16:00)
                        return hoy.plusHours(16);
                    } else if (turnoAveria.equals("T2")) {
                        // Si ocurre en T2, disponible en T1 del día siguiente (desde las 00:00)
                        return hoy.plusDays(1);
                    } else {
                        // Si ocurre en T3, disponible en T2 del día siguiente (desde las 08:00)
                        return hoy.plusDays(1).plusHours(8);
                    }

                case "TI3":
                    // 3 días después del incidente
                    return momentoAveria.toLocalDate().plusDays(3).atStartOfDay();

                default:
                    throw new IllegalArgumentException("Tipo de incidente no válido: " + tipoIncidente);
            }
        }

        /**
         * Marca una avería como aplicada
         *
         * @param averia Registro de avería a marcar
         */
        public void marcarAveriaComoAplicada(RegistroAveria averia) {
            if (averia != null) {
                averia.setAplicada(true);
            }
        }

        /**
         * Determina el punto en la ruta donde debe ocurrir la avería
         *
         * @param rutaCompleta Lista de ubicaciones que forman la ruta
         * @return Índice en la ruta donde debe ocurrir la avería
         */
        public int determinarPuntoAveria(List<Ubicacion> rutaCompleta) {
            if (rutaCompleta == null || rutaCompleta.size() <= 1) {
                return 0;
            }

            // La avería debe ocurrir entre el 5% y 35% de la ruta
            int tamanoRuta = rutaCompleta.size();
            int minIndice = Math.max(1, (int) (tamanoRuta * 0.05));
            int maxIndice = Math.min(tamanoRuta - 1, (int) (tamanoRuta * 0.35));

            // Generar un índice aleatorio dentro del rango
            return minIndice + new Random().nextInt(maxIndice - minIndice + 1);
        }

        /**
         * Actualiza el estado de un camión según sus averías
         *
         * @param camion        Camión a actualizar
         * @param momentoActual Momento actual
         */
        public void actualizarEstadoCamionSegunAverias(Camion camion, LocalDateTime momentoActual) {
            if (camion == null) return;

            // Buscar si hay registros de averías aplicadas para este camión
            if (!averiasRegistradas.containsKey(camion.getCodigo())) {
                return;
            }

            for (RegistroAveria averia : averiasRegistradas.get(camion.getCodigo())) {
                if (averia.isAplicada()) {
                    // Calcular fin de indisponibilidad
                    LocalDateTime finIndisponibilidad = calcularFinIndisponibilidad(
                            averia.getTipoIncidente(), momentoActual.minusHours(24)); // Aproximación

                    // Si aún no ha finalizado la indisponibilidad
                    if (momentoActual.isBefore(finIndisponibilidad)) {
                        // Actualizar estado del camión
                        camion.registrarAveria(com.glp.glpDP1.domain.enums.TipoIncidente.valueOf(averia.getTipoIncidente()),
                                momentoActual.minusHours(24)); // Aproximación
                        break;
                    }
                }
            }
        }
    }

    /**
     * Registra un evento de avería en la ruta
     *
     * @param ruta          Ruta donde registrar el evento
     * @param tipoIncidente Tipo de incidente (TI1, TI2, TI3)
     * @param momento       Momento en que ocurre el evento
     * @param ubicacion     Ubicación donde ocurre el evento
     */
    private void registrarEventoAveria(Ruta ruta, String tipoIncidente,
                                       LocalDateTime momento, Ubicacion ubicacion) {
        EventoRuta.TipoEvento tipoEvento;
        String descripcion;

        switch (tipoIncidente) {
            case "TI1":
                tipoEvento = EventoRuta.TipoEvento.AVERIA_TI1;
                descripcion = "Avería menor: Llanta baja (2h inmovilización)";
                break;
            case "TI2":
                tipoEvento = EventoRuta.TipoEvento.AVERIA_TI2;
                descripcion = "Avería media: Motor ahogado (2h inmovilización + 1 turno indisponible)";
                break;
            case "TI3":
                tipoEvento = EventoRuta.TipoEvento.AVERIA_TI3;
                descripcion = "Avería grave: Choque (4h inmovilización + 3 días indisponible)";
                break;
            default:
                tipoEvento = EventoRuta.TipoEvento.AVERIA_TI1; // Por defecto
                descripcion = "Avería sin clasificar";
        }

        ruta.registrarEvento(tipoEvento, momento, ubicacion, descripcion);
    }
}
