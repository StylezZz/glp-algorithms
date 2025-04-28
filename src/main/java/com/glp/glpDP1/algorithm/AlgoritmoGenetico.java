package com.glp.glpDP1.algorithm;

import com.glp.glpDP1.domain.*;
import com.glp.glpDP1.domain.enums.EstadoCamion;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter @Setter
public class AlgoritmoGenetico {

    // Parámetros del algoritmo
    private int tamañoPoblacion;
    private int numGeneraciones;
    private double tasaMutacion;
    private double tasaCruce;
    private int elitismo;

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
    public AlgoritmoGenetico() {
        this(100, 50, 0.05, 0.7, 5);
    }

    /**
     * Constructor con parámetros personalizados
     * @param tamañoPoblacion Tamaño de la población
     * @param numGeneraciones Número máximo de generaciones
     * @param tasaMutacion Probabilidad de mutación
     * @param tasaCruce Probabilidad de cruce
     * @param elitismo Número de individuos elite que pasan directamente
     */
    public AlgoritmoGenetico(int tamañoPoblacion, int numGeneraciones,
                             double tasaMutacion, double tasaCruce, int elitismo) {
        this.tamañoPoblacion = tamañoPoblacion;
        this.numGeneraciones = numGeneraciones;
        this.tasaMutacion = tasaMutacion;
        this.tasaCruce = tasaCruce;
        this.elitismo = elitismo;
    }

    /**
     * Ejecuta el algoritmo genético para encontrar la mejor asignación de rutas
     * @param camiones Lista de camiones disponibles
     * @param pedidos Lista de pedidos pendientes
     * @param mapa Mapa de la ciudad
     * @param momento Momento actual para la planificación
     * @return Lista de rutas optimizadas
     */
    public List<Ruta> optimizarRutas(List<Camion> camiones, List<Pedido> pedidos,
                                     Mapa mapa, LocalDateTime momento) {
        this.camionesDisponibles = filtrarCamionesDisponibles(camiones);
        this.pedidosPendientes = preprocesarPedidos(pedidos); // Usar preprocesamiento
        this.mapa = mapa;
        this.momentoActual = momento;

        // Si no hay camiones disponibles o pedidos pendientes, retornar lista vacía
        if (camionesDisponibles.isEmpty() || pedidosPendientes.isEmpty()) {
            return new ArrayList<>();
        }

        // Inicializar la mejor solución conocida
        mejorSolucion = null;
        mejorFitness = Double.MAX_VALUE;

        // Generar población inicial
        List<Individuo> poblacion = inicializarPoblacion();

        // Evaluar la población inicial
        evaluarPoblacion(poblacion);

        // Ciclo principal del algoritmo genético
        for (int generacion = 0; generacion < numGeneraciones; generacion++) {
            // Seleccionar individuos para reproducción
            List<Individuo> seleccionados = seleccion(poblacion);

            // Crear nueva población mediante cruces y mutaciones
            List<Individuo> nuevaPoblacion = new ArrayList<>();

            // Añadir individuos elite directamente
            for (int i = 0; i < elitismo && i < poblacion.size(); i++) {
                nuevaPoblacion.add(poblacion.get(i).clonar());
            }

            // Generar el resto de la población mediante cruce y mutación
            while (nuevaPoblacion.size() < tamañoPoblacion) {
                // Seleccionar padres
                Individuo padre = seleccionados.get(new Random().nextInt(seleccionados.size()));
                Individuo madre = seleccionados.get(new Random().nextInt(seleccionados.size()));

                // Realizar cruce con cierta probabilidad
                List<Individuo> hijos;
                if (Math.random() < tasaCruce) {
                    hijos = cruce(padre, madre);
                } else {
                    hijos = Arrays.asList(padre.clonar(), madre.clonar());
                }

                // Aplicar mutación con cierta probabilidad
                for (Individuo hijo : hijos) {
                    if (Math.random() < tasaMutacion) {
                        mutacion(hijo);
                    }

                    // Añadir a la nueva población si hay espacio
                    if (nuevaPoblacion.size() < tamañoPoblacion) {
                        nuevaPoblacion.add(hijo);
                    }
                }
            }

            // Evaluar la nueva población
            evaluarPoblacion(nuevaPoblacion);

            // Actualizar la población
            poblacion = nuevaPoblacion;

            // Verificar si se ha encontrado una mejor solución
            Individuo mejorIndividuo = poblacion.get(0);
            if (mejorIndividuo.getFitness() < mejorFitness) {
                mejorSolucion = mejorIndividuo.decodificarSolucion();
                mejorFitness = mejorIndividuo.getFitness();

                System.out.println("Gen " + generacion + ": Nuevo mejor fitness = " + mejorFitness);
            }

            // Condición de parada temprana: si el fitness no mejora en varias generaciones
            if (generacion > 20 && poblacion.get(0).getFitness() == poblacion.get(10).getFitness()) {
                System.out.println("Convergencia alcanzada en generación " + generacion);
                break;
            }
        }

        // Retornar la mejor solución encontrada
        return mejorSolucion;
    }

    private List<Pedido> preprocesarPedidos(List<Pedido> pedidosOriginales){
        List<Pedido> pedidosProcesados = new ArrayList<>();
        for(Pedido pedido: pedidosOriginales){
            if(pedido.getCantidadGLP()<=25.0){
                pedidosProcesados.add(pedido);
                continue;
            }

            double cantidadRestante = pedido.getCantidadGLP();
            int contador = 1;
            while(cantidadRestante > 0 ){
                double cantidadParte = Math.min(cantidadRestante,25.0);
                cantidadRestante -= cantidadParte;

                Pedido pedidoParte = new Pedido(
                        pedido.getIdCliente()+"_parte"+contador,
                        pedido.getUbicacion(),
                        cantidadParte,
                        pedido.getHoraRecepcion(),
                        (int)pedido.getTiempoLimiteEntrega().toHours()
                );

                pedidosProcesados.add(pedidoParte);
                contador++;
            }
        }
        return pedidosProcesados;
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
     * Inicializa una población de soluciones aleatorias
     * @return Lista de individuos (soluciones potenciales)
     */
    private List<Individuo> inicializarPoblacion() {
        List<Individuo> poblacion = new ArrayList<>();

        for (int i = 0; i < tamañoPoblacion; i++) {
            Individuo individuo = new Individuo();

            // Inicializar genes (asignación de pedidos a camiones)
            for (Pedido pedido : pedidosPendientes) {
                // Asignar a un camión aleatorio o a ninguno (valor -1)
                int indiceCamion = new Random().nextInt(camionesDisponibles.size() + 1) - 1;
                individuo.getGenes().add(indiceCamion);
            }

            poblacion.add(individuo);
        }

        return poblacion;
    }

    /**
     * Evalúa el fitness de toda la población y la ordena de mejor a peor
     * @param poblacion Lista de individuos a evaluar
     */
    private void evaluarPoblacion(List<Individuo> poblacion) {
        for (Individuo individuo : poblacion) {
            calcularFitness(individuo);
        }

        // Ordenar por fitness (menor es mejor)
        poblacion.sort(Comparator.comparingDouble(Individuo::getFitness));
    }

    private Camion getCamionPorCodigo(String codigo){
        return  camionesDisponibles.stream().filter(c->c.getCodigo().equals(codigo)).findFirst().orElse(null);
    }

    /**
     * Calcula el valor de fitness (aptitud) de un individuo
     * Menor fitness es mejor (problema de minimización)
     * @param individuo Individuo a evaluar
     */
    private void calcularFitness(Individuo individuo) {
        // Decodificar la solución para obtener las rutas
        List<Ruta> rutas = individuo.decodificarSolucion();

        double consumoTotal = 0.0;      // Consumo total de combustible
        double distanciaTotal = 0.0;    // Distancia total recorrida
        double retrasosTotal = 0.0;     // Suma de retrasos (en minutos)
        double pedidosNoAsignados = 0;  // Número de pedidos sin asignar
        double sobrecargaTotal = 0.0;   // Suma de sobrecargas de GLP en camiones

        // Evaluar cada ruta
        for (Ruta ruta : rutas) {
            // Obtener el camión asignado a esta ruta
            Camion camion = getCamionPorCodigo(ruta.getCodigoCamion());
            if(camion==null)continue;
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
            ruta.setConsumoCombustible(consumo);

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

        // Contar pedidos no asignados
        for (int i = 0; i < individuo.getGenes().size(); i++) {
            if (individuo.getGenes().get(i) == -1) {
                pedidosNoAsignados++;
            }
        }

        // Calcular fitness final (ponderado)
        double fitness = 0.3 * consumoTotal +
                0.1 * distanciaTotal +
                0.2 * retrasosTotal +
                0.3 * (pedidosNoAsignados * 1000) +  // Penalización fuerte por pedidos no asignados
                0.1 * (sobrecargaTotal * 1000);     // Penalización fuerte por sobrecarga

        individuo.setFitness(fitness);
    }

    /**
     * Selecciona individuos para reproducción mediante torneo
     * @param poblacion Población actual
     * @return Lista de individuos seleccionados
     */
    private List<Individuo> seleccion(List<Individuo> poblacion) {
        List<Individuo> seleccionados = new ArrayList<>();
        int tamañoTorneo = 3;

        // Seleccionar individuos mediante torneos
        for (int i = 0; i < tamañoPoblacion; i++) {
            List<Individuo> torneo = new ArrayList<>();

            // Seleccionar participantes aleatorios para el torneo
            for (int j = 0; j < tamañoTorneo; j++) {
                int indice = new Random().nextInt(poblacion.size());
                torneo.add(poblacion.get(indice));
            }

            // Elegir el mejor del torneo
            torneo.sort(Comparator.comparingDouble(Individuo::getFitness));
            seleccionados.add(torneo.get(0));
        }

        return seleccionados;
    }

    /**
     * Realiza el cruce entre dos individuos para generar descendencia
     * @param padre Primer individuo
     * @param madre Segundo individuo
     * @return Lista con dos nuevos individuos (hijos)
     */
    private List<Individuo> cruce(Individuo padre, Individuo madre) {
        Individuo hijo1 = new Individuo();
        Individuo hijo2 = new Individuo();

        // Punto de cruce aleatorio
        int puntoCruce = new Random().nextInt(padre.getGenes().size());

        // Generar genes de los hijos
        for (int i = 0; i < padre.getGenes().size(); i++) {
            if (i < puntoCruce) {
                hijo1.getGenes().add(padre.getGenes().get(i));
                hijo2.getGenes().add(madre.getGenes().get(i));
            } else {
                hijo1.getGenes().add(madre.getGenes().get(i));
                hijo2.getGenes().add(padre.getGenes().get(i));
            }
        }

        return Arrays.asList(hijo1, hijo2);
    }

    /**
     * Aplica mutación a un individuo
     * @param individuo Individuo a mutar
     */
    private void mutacion(Individuo individuo) {
        // Seleccionar un gen aleatorio para mutar
        int indiceMutacion = new Random().nextInt(individuo.getGenes().size());

        // Cambiar la asignación del pedido
        int nuevaAsignacion = new Random().nextInt(camionesDisponibles.size() + 1) - 1;
        individuo.getGenes().set(indiceMutacion, nuevaAsignacion);
    }

    /**
     * Clase interna que representa un individuo (una solución potencial)
     * El cromosoma está codificado como una lista de enteros, donde cada posición
     * corresponde a un pedido y el valor indica el índice del camión asignado
     * (-1 significa que el pedido no está asignado a ningún camión)
     */
    @Getter @Setter
    private class Individuo {
        private List<Integer> genes;
        private double fitness;

        public Individuo() {
            this.genes = new ArrayList<>();
            this.fitness = Double.MAX_VALUE;
        }



        /**
         * Crea una copia del individuo
         * @return Clon del individuo
         */
        public Individuo clonar() {
            Individuo clon = new Individuo();
            clon.genes = new ArrayList<>(this.genes);
            clon.fitness = this.fitness;
            return clon;
        }

        /**
         * Decodifica el cromosoma para generar las rutas correspondientes
         * @return Lista de rutas generadas
         */
        public List<Ruta> decodificarSolucion() {
            Map<String, Ruta> rutasPorCamion = new HashMap<>();

            // Para cada pedido, asignarlo a la ruta del camión correspondiente
            for (int i = 0; i < genes.size(); i++) {
                int indiceCamion = genes.get(i);

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
}
