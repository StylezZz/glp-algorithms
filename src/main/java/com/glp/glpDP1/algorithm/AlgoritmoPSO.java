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
 * Implementación del algoritmo de Optimización por Enjambre de Partículas (PSO)
 * para la optimización de rutas de distribución de GLP
 */
@Getter @Setter
public class AlgoritmoPSO {

    // Parámetros del algoritmo
    private int numParticulas;
    private int numIteraciones;
    private double w;       // Inercia
    private double c1;      // Factor de aprendizaje cognitivo
    private double c2;      // Factor de aprendizaje social

    // Datos del problema
    private List<Camion> camionesDisponibles;
    private List<Pedido> pedidosPendientes;
    private Mapa mapa;
    private LocalDateTime momentoActual;

    // Resultados
    private List<Ruta> mejorSolucion;
    private double mejorFitness;

    /**
     * Constructor con parámetros predeterminados
     */
    public AlgoritmoPSO() {
        this(50, 100, 0.7, 1.5, 1.5);
    }

    /**
     * Constructor con parámetros personalizados
     * @param numParticulas Número de partículas en el enjambre
     * @param numIteraciones Número máximo de iteraciones
     * @param w Factor de inercia
     * @param c1 Factor de aprendizaje cognitivo
     * @param c2 Factor de aprendizaje social
     */
    public AlgoritmoPSO(int numParticulas, int numIteraciones, double w, double c1, double c2) {
        this.numParticulas = numParticulas;
        this.numIteraciones = numIteraciones;
        this.w = w;
        this.c1 = c1;
        this.c2 = c2;
    }

    /**
     * Ejecuta el algoritmo PSO para encontrar la mejor asignación de rutas
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa de la ciudad
     * @param momento Momento actual para la planificación
     * @return Lista de rutas optimizadas
     */
    public List<Ruta> optimizarRutas(List<Camion> camiones, List<Pedido> pedidos,
                                     Mapa mapa, LocalDateTime momento) {
        this.camionesDisponibles = filtrarCamionesDisponibles(camiones);
        this.pedidosPendientes = new ArrayList<>(pedidos);
        this.mapa = mapa;
        this.momentoActual = momento;

        // Si no hay camiones disponibles o pedidos pendientes, retornar lista vacía
        if (camionesDisponibles.isEmpty() || pedidosPendientes.isEmpty()) {
            return new ArrayList<>();
        }

        // Inicializar la mejor solución conocida
        mejorSolucion = null;
        mejorFitness = Double.MAX_VALUE;

        // Inicializar el enjambre de partículas
        List<Particula> enjambre = inicializarEnjambre();

        // Evaluar el enjambre inicial
        for (Particula particula : enjambre) {
            evaluarParticula(particula);
            particula.actualizarMejorPersonal();
        }

        // Encontrar la mejor solución global inicial
        Particula mejorParticulaGlobal = obtenerMejorParticula(enjambre);

        // Ciclo principal del algoritmo PSO
        for (int iteracion = 0; iteracion < numIteraciones; iteracion++) {
            // Factor de inercia adaptativo (decrece linealmente)
            double wActual = w - ((double)iteracion / numIteraciones) * (w - 0.4);

            // Actualizar cada partícula
            for (Particula particula : enjambre) {
                // Actualizar velocidad y posición
                particula.actualizarVelocidad(mejorParticulaGlobal, wActual, c1, c2);
                particula.actualizarPosicion();

                // Evaluar la nueva posición
                evaluarParticula(particula);

                // Actualizar mejor posición personal
                particula.actualizarMejorPersonal();
            }

            // Actualizar mejor solución global
            Particula nuevaMejorGlobal = obtenerMejorParticula(enjambre);
            if (nuevaMejorGlobal.getMejorFitness() < mejorParticulaGlobal.getMejorFitness()) {
                mejorParticulaGlobal = nuevaMejorGlobal;
                mejorSolucion = mejorParticulaGlobal.decodificarSolucion();
                mejorFitness = mejorParticulaGlobal.getMejorFitness();

                System.out.println("Iter " + iteracion + ": Nuevo mejor fitness = " + mejorFitness);
            }

            // Condición de parada temprana: convergencia
            if (iteracion > 20 && Math.abs(mejorFitness - mejorParticulaGlobal.getMejorFitness()) < 0.001) {
                System.out.println("Convergencia alcanzada en iteración " + iteracion);
                break;
            }
        }

        // Retornar la mejor solución encontrada
        return mejorSolucion;
    }

    /**
     * Filtra los camiones que están disponibles para asignación
     * @param camiones Lista de todos los camiones
     * @return Lista de camiones disponibles
     */
    private List<Camion> filtrarCamionesDisponibles(List<Camion> camiones) {
        return camiones.stream()
                .filter(c -> c.getEstado() == EstadoCamion.DISPONIBLE)
                .collect(Collectors.toList());
    }

    /**
     * Inicializa un enjambre de partículas aleatorias
     * @return Lista de partículas
     */
    private List<Particula> inicializarEnjambre() {
        List<Particula> enjambre = new ArrayList<>();

        for (int i = 0; i < numParticulas; i++) {
            Particula particula = new Particula(pedidosPendientes.size(), camionesDisponibles.size());

            // Inicializar posición aleatoria (asignación de pedidos a camiones)
            for (int j = 0; j < pedidosPendientes.size(); j++) {
                double posicion = Math.random() * (camionesDisponibles.size() + 1) - 0.5;
                particula.getPosicion()[j] = posicion;

                // Inicializar velocidad aleatoria
                particula.getVelocidad()[j] = Math.random() - 0.5;
            }

            enjambre.add(particula);
        }

        return enjambre;
    }

    /**
     * Evalúa el fitness de una partícula
     * @param particula Partícula a evaluar
     */
    private void evaluarParticula(Particula particula) {
        // Decodificar la solución para obtener las rutas
        List<Ruta> rutas = particula.decodificarSolucion();

        double consumoTotal = 0.0;      // Consumo total de combustible
        double distanciaTotal = 0.0;    // Distancia total recorrida
        double retrasosTotal = 0.0;     // Suma de retrasos (en minutos)
        double pedidosNoAsignados = 0;  // Número de pedidos sin asignar
        double sobrecargaTotal = 0.0;   // Suma de sobrecargas de GLP en camiones

        // Evaluar cada ruta
        for (Ruta ruta : rutas) {
            // Obtener el camión asignado a esta ruta
            Camion camion = null;
            for (Camion c : camionesDisponibles) {
                if (c.getCodigo().equals(ruta.getCodigoCamion())) {
                    camion = c;
                    break;
                }
            }

            if (camion == null) {
                continue; // Esto no debería ocurrir
            }

            // Calcular consumo de combustible
            double consumo = ruta.calcularConsumoCombustible(camion);
            consumoTotal += consumo;

            // Sumar distancia total
            distanciaTotal += ruta.getDistanciaTotal();

            // Verificar capacidad GLP
            double glpTotal = 0;
            for (Pedido p : ruta.getPedidosAsignados()) {
                glpTotal += p.getCantidadGLP();
            }

            if (glpTotal > camion.getCapacidadTanqueGLP()) {
                sobrecargaTotal += (glpTotal - camion.getCapacidadTanqueGLP());
            }

            // Calcular retrasos potenciales
            for (Pedido pedido : ruta.getPedidosAsignados()) {
                // Estimar tiempo de entrega basado en distancia y velocidad
                Ubicacion origen = camion.getUbicacionActual();
                int distanciaAlPedido = ((Ubicacion) origen).distanciaA(pedido.getUbicacion());
                double horasViaje = distanciaAlPedido / 50.0; // 50 km/h velocidad promedio

                LocalDateTime entregaEstimada = momentoActual.plusMinutes((long)(horasViaje * 60));

                // Si entrega estimada es posterior a la hora límite, hay retraso
                if (entregaEstimada.isAfter(pedido.getHoraLimiteEntrega())) {
                    Duration retraso = Duration.between(pedido.getHoraLimiteEntrega(), entregaEstimada);
                    retrasosTotal += retraso.toMinutes();
                }
            }
        }

        // Contar pedidos no asignados
        int[] asignaciones = particula.decodificarAsignaciones();
        for (int asignacion : asignaciones) {
            if (asignacion == -1) {
                pedidosNoAsignados++;
            }
        }

        // Calcular fitness final (ponderado)
        double fitness = 0.3 * consumoTotal +
                0.1 * distanciaTotal +
                0.2 * retrasosTotal +
                0.3 * (pedidosNoAsignados * 1000) +  // Penalización fuerte por pedidos no asignados
                0.1 * (sobrecargaTotal * 1000);     // Penalización fuerte por sobrecarga

        particula.setFitness(fitness);
    }

    /**
     * Obtiene la mejor partícula del enjambre
     * @param enjambre Lista de partículas
     * @return La mejor partícula (con menor fitness)
     */
    private Particula obtenerMejorParticula(List<Particula> enjambre) {
        return enjambre.stream()
                .min(Comparator.comparingDouble(Particula::getMejorFitness))
                .orElseThrow(() -> new RuntimeException("Enjambre vacío"));
    }

    /**
     * Clase interna que representa una partícula en el enjambre
     */
    @Getter @Setter
    private class Particula {
        private double[] posicion;     // Posición actual
        private double[] velocidad;    // Velocidad actual
        private double[] mejorPosicion; // Mejor posición personal
        private double fitness;        // Fitness actual
        private double mejorFitness;   // Mejor fitness personal
        private final int numCamiones; // Número de camiones disponibles

        public Particula(int numPedidos, int numCamiones) {
            this.posicion = new double[numPedidos];
            this.velocidad = new double[numPedidos];
            this.mejorPosicion = new double[numPedidos];
            this.fitness = Double.MAX_VALUE;
            this.mejorFitness = Double.MAX_VALUE;
            this.numCamiones = numCamiones;
        }

        /**
         * Actualiza la mejor posición personal si la actual es mejor
         */
        public void actualizarMejorPersonal() {
            if (fitness < mejorFitness) {
                mejorFitness = fitness;
                System.arraycopy(posicion, 0, mejorPosicion, 0, posicion.length);
            }
        }

        /**
         * Actualiza la velocidad de la partícula según la fórmula PSO
         * @param mejorGlobal Mejor partícula global
         * @param w Factor de inercia
         * @param c1 Factor de aprendizaje cognitivo
         * @param c2 Factor de aprendizaje social
         */
        public void actualizarVelocidad(Particula mejorGlobal, double w, double c1, double c2) {
            Random random = new Random();

            for (int i = 0; i < velocidad.length; i++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                // Componente de inercia
                double inercia = w * velocidad[i];

                // Componente cognitiva (atracción hacia la mejor posición personal)
                double cognitiva = c1 * r1 * (mejorPosicion[i] - posicion[i]);

                // Componente social (atracción hacia la mejor posición global)
                double social = c2 * r2 * (mejorGlobal.mejorPosicion[i] - posicion[i]);

                // Nueva velocidad
                velocidad[i] = inercia + cognitiva + social;

                // Limitar la velocidad
                if (velocidad[i] > 1.0) velocidad[i] = 1.0;
                if (velocidad[i] < -1.0) velocidad[i] = -1.0;
            }
        }

        /**
         * Actualiza la posición de la partícula según su velocidad
         */
        public void actualizarPosicion() {
            for (int i = 0; i < posicion.length; i++) {
                posicion[i] += velocidad[i];

                // Mantener la posición dentro de los límites
                if (posicion[i] > numCamiones + 0.5) posicion[i] = numCamiones + 0.5;
                if (posicion[i] < -0.5) posicion[i] = -0.5;
            }
        }

        /**
         * Decodifica la posición para determinar las asignaciones de pedidos a camiones
         * @return Array de asignaciones (-1 si no está asignado)
         */
        public int[] decodificarAsignaciones() {
            int[] asignaciones = new int[posicion.length];

            for (int i = 0; i < posicion.length; i++) {
                // Redondear la posición al entero más cercano
                int asignacion = (int) Math.round(posicion[i]);

                // Ajustar los límites
                if (asignacion < 0) asignacion = -1;
                if (asignacion >= numCamiones) asignacion = numCamiones - 1;

                asignaciones[i] = asignacion;
            }

            return asignaciones;
        }

        /**
         * Decodifica la posición para generar las rutas correspondientes
         * @return Lista de rutas generadas
         */
        public List<Ruta> decodificarSolucion() {
            Map<String, Ruta> rutasPorCamion = new HashMap<>();
            int[] asignaciones = decodificarAsignaciones();

            // Para cada pedido, asignarlo a la ruta del camión correspondiente
            for (int i = 0; i < asignaciones.length; i++) {
                int indiceCamion = asignaciones[i];

                // Si el pedido no está asignado, continuar
                if (indiceCamion == -1) {
                    continue;
                }

                // Asegurarse de que el índice sea válido
                if (indiceCamion >= camionesDisponibles.size()) {
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
    }

    // Getters y setters para los parámetros del algoritmo
}
