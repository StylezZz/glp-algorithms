package com.glp.glpDP1.services;

import com.glp.glpDP1.domain.Bloqueo;
import com.glp.glpDP1.domain.Pedido;

import java.io.InputStream;
import java.util.List;

public interface FileService {
    /**
     * Carga y procesa un archivo de pedidos (ventas)
     * @param inputStream Stream con el contenido del archivo
     * @return Lista de pedidos procesados
     */
    List<Pedido> cargarPedidos(InputStream inputStream, String nombreArchivo);

    /**
     * Carga y procesa un archivo de bloqueos
     * @param inputStream Stream con el contenido del archivo
     * @return Lista de bloqueos procesados
     */
    List<Bloqueo> cargarBloqueos(InputStream inputStream);
}