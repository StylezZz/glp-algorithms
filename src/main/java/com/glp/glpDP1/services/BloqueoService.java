package com.glp.glpDP1.services;

import com.glp.glpDP1.persistence.entity.BloqueoEntity;
import com.glp.glpDP1.repository.BloqueoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;

    public BloqueoService(BloqueoRepository bloqueoRepository) {
        this.bloqueoRepository = bloqueoRepository;
    }

    public void procesarArchivoBloqueos(MultipartFile file) throws IOException {
        List<String> lineas = new BufferedReader(new InputStreamReader(file.getInputStream()))
                .lines()
                .collect(Collectors.toList());

        Map<String, Integer> agrupaciones = new HashMap<>();
        int[] contadorGrupo = {1};

        for (String linea : lineas) {
            try {
                procesarLineaBloqueo(linea, agrupaciones, contadorGrupo);
            } catch (Exception e) {
                System.err.println("Error al procesar línea: " + linea + " → " + e.getMessage());
            }
        }
    }

    private void procesarLineaBloqueo(String linea, Map<String, Integer> agrupaciones, int[] contadorGrupo) {
        String[] partes = linea.split(":");
        if (partes.length != 2)
            throw new RuntimeException("Formato incorrecto (falta ':')");

        String rango = partes[0].trim();
        String[] coordenadas = partes[1].split(",");

        if (coordenadas.length % 2 != 0)
            throw new RuntimeException("Número impar de coordenadas en la línea");

        String[] tiempos = rango.split("[–-]");
        if (tiempos.length != 2)
            throw new RuntimeException("Formato incorrecto del rango de tiempo");

        int[] inicio = parsearTiempo(tiempos[0]);
        int[] fin = parsearTiempo(tiempos[1]);

        // Obtener o asignar flag_mes_agrupa
        int flagMesAgrupa;
        if (agrupaciones.containsKey(rango)) {
            flagMesAgrupa = agrupaciones.get(rango);
        } else {
            flagMesAgrupa = contadorGrupo[0]++;
            agrupaciones.put(rango, flagMesAgrupa);
        }

        for (int i = 0; i < coordenadas.length; i += 2) {
            int x = Integer.parseInt(coordenadas[i].trim());
            int y = Integer.parseInt(coordenadas[i + 1].trim());

            BloqueoEntity entity = new BloqueoEntity();
            entity.setDiaInicio(inicio[0]);
            entity.setHoraInicio(inicio[1]);
            entity.setMinutoInicio(inicio[2]);

            entity.setDiaFin(fin[0]);
            entity.setHoraFin(fin[1]);
            entity.setMinutoFin(fin[2]);

            entity.setUbicacionX(x);
            entity.setUbicacionY(y);
            entity.setFlagMesAgrupa(flagMesAgrupa);
            entity.setFlagInternoAgrupa(0); // Por ahora puedes dejarlo en 0

            bloqueoRepository.save(entity);
        }
    }

    private int[] parsearTiempo(String str) {
        str = str.replace("d", " ")
                .replace("h", " ")
                .replace("m", " ");
        String[] partes = str.trim().split("\\s+");

        return new int[]{
                Integer.parseInt(partes[0].replaceFirst("^0+(?!$)", "")),
                Integer.parseInt(partes[1]),
                Integer.parseInt(partes[2])
        };
    }
}
