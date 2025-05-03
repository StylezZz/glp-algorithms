package com.glp.glpDP1.domain;

import com.glp.glpDP1.domain.enums.TipoAlmacen;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class Mapa {
    private final int ancho;      // Dimensión en el eje X (km)
    private final int alto;       // Dimensión en el eje Y (km)
    private final List<Bloqueo> bloqueos;
    private final List<Almacen> almacenes;

    // Constructor por defecto, usa los valores del enunciado
    public Mapa() {
        this(70, 50);
    }

    public Mapa(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.bloqueos = new ArrayList<>();
        this.almacenes = new ArrayList<>();

        // Inicializar almacenes predeterminados
        inicializarAlmacenes();
    }

    private void inicializarAlmacenes() {
        // Almacén central: posición X=12, Y=8
        almacenes.add(new Almacen("CENTRAL", new Ubicacion(12, 8),
                TipoAlmacen.PRINCIPAL, Double.MAX_VALUE));

        // Almacén intermedio Norte: posición X=42, Y=42
        almacenes.add(new Almacen("NORTE", new Ubicacion(42, 42),
                TipoAlmacen.INTERMEDIO, 160.0));

        // Almacén intermedio Este: posición X=63, Y=3
        almacenes.add(new Almacen("ESTE", new Ubicacion(63, 3),
                TipoAlmacen.INTERMEDIO, 160.0));
    }

    public List<Bloqueo> getBloqueos() {
        return new ArrayList<>(bloqueos);
    }

    public void setBloqueos(List<Bloqueo> bloqueos) {
        this.bloqueos.clear();
        this.bloqueos.addAll(bloqueos);
    }

    public void agregarBloqueo(Bloqueo bloqueo) {
        bloqueos.add(bloqueo);
    }

    public List<Almacen> getAlmacenes() {
        return new ArrayList<>(almacenes);
    }

    /**
     * Verifica si un nodo está dentro de los límites del mapa
     * @param ubicacion Ubicación a verificar
     * @return true si la ubicación es válida
     */
    public boolean esUbicacionValida(Ubicacion ubicacion) {
        return ubicacion.getX() >= 0 && ubicacion.getX() <= ancho &&
                ubicacion.getY() >= 0 && ubicacion.getY() <= alto;
    }

    /**
     * Verifica si un nodo está bloqueado en un momento dado
     * @param ubicacion Ubicación a verificar
     * @param momento Momento para la verificación
     * @return true si la ubicación está bloqueada
     */
    public boolean estaBloqueado(Ubicacion ubicacion, LocalDateTime momento) {
        for (Bloqueo bloqueo : bloqueos) {
            if (bloqueo.estaBloqueado(ubicacion, momento)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si un tramo entre dos nodos está bloqueado
     * @param origen Nodo de origen
     * @param destino Nodo de destino
     * @param momento Momento para la verificación
     * @return true si el tramo está bloqueado
     */
    public boolean tramoBloqueado(Ubicacion origen, Ubicacion destino, LocalDateTime momento) {
        // Verificar que son nodos adyacentes (distancia = 1)
        if (origen.distanciaA(destino) != 1) {
            return false;
        }

        for (Bloqueo bloqueo : bloqueos) {
            if (bloqueo.tramoBloqueado(origen, destino, momento)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene el almacén más cercano a una ubicación
     * @param ubicacion Ubicación de referencia
     * @return Almacén más cercano
     */
    public Almacen obtenerAlmacenMasCercano(Ubicacion ubicacion) {
        Almacen masCercano = null;
        int distanciaMinima = Integer.MAX_VALUE;

        for (Almacen almacen : almacenes) {
            int distancia = ubicacion.distanciaA(almacen.getUbicacion());
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = almacen;
            }
        }

        return masCercano;
    }

    /**
     * Obtiene el almacén central (principal)
     * @return Almacén principal
     */
    public Almacen obtenerAlmacenCentral() {
        for (Almacen almacen : almacenes) {
            if (almacen.getTipo() == TipoAlmacen.PRINCIPAL) {
                return almacen;
            }
        }
        return null;
    }

    /**
     * Obtiene los almacenes intermedios
     * @return Lista de almacenes intermedios
     */
    public List<Almacen> obtenerAlmacenesIntermedios() {
        List<Almacen> intermedios = new ArrayList<>();
        for (Almacen almacen : almacenes) {
            if (almacen.getTipo() == TipoAlmacen.INTERMEDIO) {
                intermedios.add(almacen);
            }
        }
        return intermedios;
    }

    /**
     * Encuentra la ruta más corta entre dos ubicaciones considerando bloqueos
     * Implementación de algoritmo A* (A-star)
     *
     * @param origen Ubicación de origen
     * @param destino Ubicación de destino
     * @param momento Momento en que se realiza el recorrido
     * @return Lista de ubicaciones que forman la ruta, o lista vacía si no hay ruta posible
     */
    public List<Ubicacion> encontrarRuta(Ubicacion origen, Ubicacion destino, LocalDateTime momento) {
        // Si origen o destino están bloqueados, no hay ruta
        if (estaBloqueado(origen, momento) || estaBloqueado(destino, momento)) {
            return new ArrayList<>();
        }

        // Si origen y destino son iguales, la ruta es el propio punto
        if (origen.equals(destino)) {
            List<Ubicacion> rutaSimple = new ArrayList<>();
            rutaSimple.add(origen);
            return rutaSimple;
        }

        // Conjunto de nodos visitados
        Set<Ubicacion> visitados = new HashSet<>();

        // Cola de prioridad para los nodos a explorar (ordenados por f = g + h)
        PriorityQueue<NodoRuta> abiertos = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.g + n.h)
        );

        // Mapa para reconstruir la ruta
        Map<Ubicacion, Ubicacion> padres = new HashMap<>();

        // Mapa de valores g (costo real hasta el nodo)
        Map<Ubicacion, Integer> valoresG = new HashMap<>();

        // Inicializar con el nodo origen
        abiertos.add(new NodoRuta(origen, 0, origen.distanciaA(destino)));
        valoresG.put(origen, 0);

        while (!abiertos.isEmpty()) {
            // Extraer el nodo con menor f = g + h
            NodoRuta nodoActual = abiertos.poll();
            Ubicacion actual = nodoActual.ubicacion;

            // Si llegamos al destino, reconstruir y devolver la ruta
            if (actual.equals(destino)) {
                return reconstruirRuta(padres, destino);
            }

            // Marcar como visitado
            visitados.add(actual);

            // Explorar los vecinos en las 8 direcciones para mayor flexibilidad
            List<Ubicacion> vecinos = obtenerVecinos(actual);

            for (Ubicacion vecino : vecinos) {
                // Verificar si el vecino es válido (dentro del mapa y no bloqueado)
                if (!esUbicacionValida(vecino) ||
                        estaBloqueado(vecino, momento) ||
                        tramoBloqueado(actual, vecino, momento) ||
                        visitados.contains(vecino)) {
                    continue;
                }

                // Calcular el nuevo valor de g (costo real)
                int nuevoG = valoresG.get(actual) + 1;  // Costo de moverse a un vecino adyacente = 1

                // Si no está en la lista abierta o encontramos un camino mejor
                if (!valoresG.containsKey(vecino) || nuevoG < valoresG.get(vecino)) {
                    // Actualizar el mapa de padres
                    padres.put(vecino, actual);

                    // Actualizar el valor de g
                    valoresG.put(vecino, nuevoG);

                    // Calcular la heurística (distancia Manhattan al destino)
                    int h = vecino.distanciaA(destino);

                    // Añadir a la lista abierta
                    abiertos.add(new NodoRuta(vecino, nuevoG, h));
                }
            }
        }

        // Si llegamos aquí, no hay ruta posible
        return new ArrayList<>();
    }

    /**
     * Encuentra la ruta más corta considerando bloqueos en los tiempos futuros de llegada
     * @param origen Ubicación de origen
     * @param destino Ubicación de destino
     * @param momentoInicio Momento de inicio del recorrido
     * @param velocidadKmH Velocidad del camión en km/h
     * @return Lista de ubicaciones que forman la ruta, o lista vacía si no hay ruta posible
     */
    public List<Ubicacion> encontrarRutaConTiempo(Ubicacion origen, Ubicacion destino,
                                                  LocalDateTime momentoInicio, double velocidadKmH) {
        // Si origen y destino son iguales, la ruta es el propio punto
        if (origen.equals(destino)) {
            List<Ubicacion> rutaSimple = new ArrayList<>();
            rutaSimple.add(origen);
            return rutaSimple;
        }

        // Si origen está bloqueado en el momento inicial, no hay ruta
        if (estaBloqueado(origen, momentoInicio)) {
            return new ArrayList<>();
        }

        // Conjunto de nodos visitados
        Set<Ubicacion> visitados = new HashSet<>();

        // Cola de prioridad para los nodos a explorar (ordenados por f = g + h)
        PriorityQueue<NodoRutaTemporal> abiertos = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.g + n.h)
        );

        // Mapa para reconstruir la ruta
        Map<Ubicacion, Ubicacion> padres = new HashMap<>();

        // Mapa de valores g (costo real hasta el nodo)
        Map<Ubicacion, Integer> valoresG = new HashMap<>();

        // Mapa de tiempos de llegada a cada nodo
        Map<Ubicacion, LocalDateTime> tiemposLlegada = new HashMap<>();

        // Inicializar con el nodo origen
        abiertos.add(new NodoRutaTemporal(origen, 0, origen.distanciaA(destino), momentoInicio));
        valoresG.put(origen, 0);
        tiemposLlegada.put(origen, momentoInicio);

        while (!abiertos.isEmpty()) {
            // Extraer el nodo con menor f = g + h
            NodoRutaTemporal nodoActual = abiertos.poll();
            Ubicacion actual = nodoActual.ubicacion;
            LocalDateTime tiempoActual = nodoActual.tiempoLlegada;

            // Si llegamos al destino, reconstruir y devolver la ruta
            if (actual.equals(destino)) {
                return reconstruirRuta(padres, destino);
            }

            // Marcar como visitado
            visitados.add(actual);

            // Explorar los vecinos en las 4 direcciones
            List<Ubicacion> vecinos = obtenerVecinos(actual);

            for (Ubicacion vecino : vecinos) {
                // Calcular tiempo de llegada al vecino
                long segundosViaje = (long) (1.0 / velocidadKmH * 3600); // 1 km a la velocidad dada
                LocalDateTime tiempoLlegadaVecino = tiempoActual.plusSeconds(segundosViaje);

                // Verificar si el vecino es válido (dentro del mapa y no bloqueado en el momento de llegada)
                if (!esUbicacionValida(vecino) ||
                        estaBloqueado(vecino, tiempoLlegadaVecino) ||
                        tramoBloqueado(actual, vecino, tiempoLlegadaVecino) ||
                        visitados.contains(vecino)) {
                    continue;
                }

                // Calcular el nuevo valor de g (costo real)
                int nuevoG = valoresG.get(actual) + 1;  // Costo de moverse a un vecino adyacente = 1

                // Si no está en la lista abierta o encontramos un camino mejor
                if (!valoresG.containsKey(vecino) || nuevoG < valoresG.get(vecino)) {
                    // Actualizar el mapa de padres
                    padres.put(vecino, actual);

                    // Actualizar el valor de g
                    valoresG.put(vecino, nuevoG);

                    // Actualizar tiempo de llegada
                    tiemposLlegada.put(vecino, tiempoLlegadaVecino);

                    // Calcular la heurística (distancia Manhattan al destino)
                    int h = vecino.distanciaA(destino);

                    // Añadir a la lista abierta
                    abiertos.add(new NodoRutaTemporal(vecino, nuevoG, h, tiempoLlegadaVecino));
                }
            }
        }

        // Si llegamos aquí, no hay ruta posible
        return new ArrayList<>();
    }

    /**
     * Clase auxiliar para el algoritmo A* con tiempo
     */
    private static class NodoRutaTemporal {
        private final Ubicacion ubicacion;
        private final int g;  // Costo real desde origen
        private final int h;  // Heurística (estimación) hasta destino
        private final LocalDateTime tiempoLlegada;  // Tiempo estimado de llegada

        public NodoRutaTemporal(Ubicacion ubicacion, int g, int h, LocalDateTime tiempoLlegada) {
            this.ubicacion = ubicacion;
            this.g = g;
            this.h = h;
            this.tiempoLlegada = tiempoLlegada;
        }
    }

    // Método auxiliar para obtener todos los vecinos posibles
    private List<Ubicacion> obtenerVecinos(Ubicacion ubicacion) {
        List<Ubicacion> vecinos = new ArrayList<>();

        // Movimientos en las cuatro direcciones principales
        vecinos.add(new Ubicacion(ubicacion.getX() + 1, ubicacion.getY()));  // Derecha
        vecinos.add(new Ubicacion(ubicacion.getX() - 1, ubicacion.getY()));  // Izquierda
        vecinos.add(new Ubicacion(ubicacion.getX(), ubicacion.getY() + 1));  // Arriba
        vecinos.add(new Ubicacion(ubicacion.getX(), ubicacion.getY() - 1));  // Abajo

        return vecinos;
    }

    /**
     * Reconstruye la ruta desde el origen hasta el destino usando el mapa de padres
     * @param padres Mapa de padres
     * @param destino Ubicación de destino
     * @return Lista de ubicaciones que forman la ruta
     */
    private List<Ubicacion> reconstruirRuta(Map<Ubicacion, Ubicacion> padres, Ubicacion destino) {
        List<Ubicacion> ruta = new ArrayList<>();
        Ubicacion actual = destino;

        while (padres.containsKey(actual)) {
            ruta.add(0, actual);
            actual = padres.get(actual);
        }

        // Añadir el origen
        ruta.add(0, actual);

        return ruta;
    }

    /**
     * Clase auxiliar para el algoritmo A*
     */
    private static class NodoRuta {
        private final Ubicacion ubicacion;
        private final int g;  // Costo real desde origen
        private final int h;  // Heurística (estimación) hasta destino

        public NodoRuta(Ubicacion ubicacion, int g, int h) {
            this.ubicacion = ubicacion;
            this.g = g;
            this.h = h;
        }
    }
}
