package com.glp.glpDP1.services;

import com.glp.glpDP1.api.dto.request.PedidoRequest;
import com.glp.glpDP1.api.dto.response.PedidoResponse;
import com.glp.glpDP1.domain.Pedido;
import com.glp.glpDP1.domain.Ubicacion;
import com.glp.glpDP1.persistence.mapper.PedidoDtoMapper;
import com.glp.glpDP1.persistence.entity.PedidoEntity;
import com.glp.glpDP1.persistence.mapper.PedidoMapper;
import com.glp.glpDP1.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public PedidoResponse guardarPedido(PedidoRequest request) {
        Pedido pedido = PedidoDtoMapper.toDomain(request);
        PedidoEntity entity = PedidoDtoMapper.toEntity(pedido); // usa el mapper correcto
        PedidoEntity saved = pedidoRepository.save(entity);
        return PedidoDtoMapper.toResponse(PedidoDtoMapper.toDomain(saved));
    }


    // Listar todos los pedidos como Response
    public List<PedidoResponse> listarPedidos() {
        return pedidoRepository.findAll().stream()
                .map(PedidoMapper::toDomain)
                .map(PedidoDtoMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 🟩 Buscar pedidos por ID de cliente
    public List<PedidoResponse> buscarPorCliente(String idCliente) {
        return pedidoRepository.findByIdCliente(idCliente).stream()
                .map(PedidoMapper::toDomain)
                .map(PedidoDtoMapper::toResponse)
                .collect(Collectors.toList());
    }


    public void procesarArchivoPedidos(MultipartFile file) throws IOException {
        List<String> lineas = new BufferedReader(new InputStreamReader(file.getInputStream()))
                .lines()
                .collect(Collectors.toList());

        for (String linea : lineas) {
            Pedido pedido = parsearLineaPedido(linea);

            // 🔄 Convertir Pedido (domain) → PedidoEntity (persistence)
            PedidoEntity entity = PedidoDtoMapper.toEntity(pedido);

            // 💾 Guardar en base de datos
            pedidoRepository.save(entity);
        }
    }

    private Pedido parsearLineaPedido(String linea) {
        try {
            String[] partes = linea.split(":");
            String momento = partes[0]; // ejemplo: 01d00h10m

            String[] datos = partes[1].split(",");
            int posX = Integer.parseInt(datos[0]);
            int posY = Integer.parseInt(datos[1]);
            String idCliente = datos[2];                // ejemplo: c-145
            double cantidadGLP = Double.parseDouble(datos[3].replace("m3", ""));
            int horasLimite = Integer.parseInt(datos[4].replace("h", ""));

            Ubicacion ubicacion = new Ubicacion(posX, posY);  // asegúrate que existe ese constructor

            return new Pedido(idCliente, ubicacion, cantidadGLP, momento, horasLimite);

        } catch (Exception e) {
            throw new RuntimeException("Error al parsear línea: " + linea, e);
        }
    }
}
