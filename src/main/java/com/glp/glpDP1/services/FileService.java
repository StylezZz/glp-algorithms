package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.enums.TipoIncidente;
import com.glp.glpDP1.domain.enums.Turno;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FileService {
    /**
     * Carga y procesa un archivo de pedidos (ventas)
     * @param inputStream Stream con el contenido del archivo
     * @return Lista de pedidos procesados
     */
    List<Pedido> cargarPedidos(InputStream inputStream);

    /**
     * Carga y procesa un archivo de pedidos para un día específico
     * @param inputStream Stream con el contenido del archivo
     * @param fechaReferencia Fecha de referencia para filtrar los pedidos
     * @return Lista de pedidos filtrados para el día especificado
     */
    List<Pedido> cargarPedidosPorDia(InputStream inputStream, LocalDateTime fechaReferencia);

    /**
     * Carga y procesa un archivo de bloqueos
     * @param inputStream Stream con el contenido del archivo
     * @return Lista de bloqueos procesados
     */
    List<Bloqueo> cargarBloqueos(InputStream inputStream);

    /**
     * Carga las averías desde un archivo
     * @param inputStream Stream del archivo de averías
     * @return Número de averías cargadas
     */
    int cargarAverias(InputStream inputStream);

    /**
     * Verifica si un camión tiene programada una avería para el turno actual
     * @param codigoCamion Código del camión
     * @param momento Momento actual
     * @return true si tiene avería programada, false en caso contrario
     */
    boolean tieneProgramadaAveria(String codigoCamion, LocalDateTime momento);

    /**
     * Obtiene el tipo de incidente programado para un camión en un turno específico
     * @param codigoCamion Código del camión
     * @param turno Turno a verificar
     * @return El tipo de incidente, o null si no hay avería programada
     */
    TipoIncidente obtenerIncidenteProgramado(String codigoCamion, Turno turno);

    /**
     * Registra que una avería ya ocurrió para evitar duplicados
     * @param codigoCamion Código del camión
     * @param momento Momento actual
     */
    void registrarAveriaOcurrida(String codigoCamion, LocalDateTime momento);

    /**
     * Limpia las averías registradas (útil para reiniciar simulaciones)
     */
    void limpiarAverias();

    /**
     * Lista todas las averías programadas (útil para diagnóstico)
     * @return Lista de mapas con información de averías
     */
    List<Map<String, String>> listarAveriasProgramadas();
}