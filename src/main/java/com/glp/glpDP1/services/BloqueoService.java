package com.glp.glpDP1.services;

import com.glp.glpDP1.persistence.entity.BloqueoEntity;
import com.glp.glpDP1.repository.BloqueoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio que gestiona bloqueos:
 *   1) limpiar la tabla
 *   2) cargar bloqueos desde un .txt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;

    /* ------------------------------------------------------------------ */
    /* 1) Vaciar tabla                                                    */
    /* ------------------------------------------------------------------ */
    @Transactional
    public void limpiarBloqueos() {
        bloqueoRepository.deleteAllInBatch();
        log.info("🧹 Tabla BLOQUEO limpia");
    }

    /* ------------------------------------------------------------------ */
    /* 2) Cargar archivo                                                   */
    /* ------------------------------------------------------------------ */
    @Transactional
    public int cargarBloqueos(MultipartFile file) throws IOException {

        limpiarBloqueos();

        Map<String, Integer> agrupaciones = new HashMap<>();    // rango → flag_mes_agrupa
        AtomicInteger correlativo = new AtomicInteger(1);       // contador thread-safe
        List<BloqueoEntity> batch = new ArrayList<>();

        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String linea;
            while ((linea = br.readLine()) != null) {

                /* ────────── saltar vacíos / comentarios ────────── */
                if (linea.isBlank() || linea.startsWith("#")) continue;

                /* 1️⃣  Split rango vs. coordenadas */
                String[] partes = linea.split(":");
                if (partes.length != 2)
                    throw new IllegalArgumentException("Formato incorrecto (falta ':') → " + linea);

                String rango     = partes[0].trim();
                String[] coords  = partes[1].split(",");

                if (coords.length % 2 != 0)
                    throw new IllegalArgumentException("Coordenadas impares → " + linea);

                /* 2️⃣  Parseo de tiempos 0d02h00m–0d02h30m  */
                String[] tiempos = rango.split("[–-]");          // admite guión normal o largo
                if (tiempos.length != 2)
                    throw new IllegalArgumentException("Rango de tiempo inválido → " + linea);

                int[] ini = parsearTiempo(tiempos[0]);
                int[] fin = parsearTiempo(tiempos[1]);

                /* 3️⃣  flag_mes_agrupa */
                int flagGrupo = agrupaciones.computeIfAbsent(
                        rango,
                        k -> correlativo.getAndIncrement()
                );

                /* 4️⃣  Generar un entity por cada (x,y) */
                for (int i = 0; i < coords.length; i += 2) {

                    int x = Integer.parseInt(coords[i].trim());
                    int y = Integer.parseInt(coords[i + 1].trim());

                    BloqueoEntity b = new BloqueoEntity();
                    b.setDiaInicio      (ini[0]);
                    b.setHoraInicio     (ini[1]);
                    b.setMinutoInicio   (ini[2]);

                    b.setDiaFin         (fin[0]);
                    b.setHoraFin        (fin[1]);
                    b.setMinutoFin      (fin[2]);

                    b.setUbicacionX(x);
                    b.setUbicacionY(y);

                    b.setFlagMesAgrupa     (flagGrupo);
                    b.setFlagInternoAgrupa (0);       // por ahora 0

                    batch.add(b);
                }
            }
        }

        bloqueoRepository.saveAll(batch);
        log.info("📌 Bloqueos cargados: {}", batch.size());
        return batch.size();
    }

    /* ------------------------------------------------------------------ */
    /* 3) Helper de tiempo  ej. "0d02h30m"                                */
    /* ------------------------------------------------------------------ */
    private int[] parsearTiempo(String s) {

        String clean = s.replace("d"," ")
                .replace("h"," ")
                .replace("m"," ")
                .trim();

        String[] p = clean.split("\\s+");
        if (p.length != 3)
            throw new IllegalArgumentException("Tiempo mal formado: " + s);

        return new int[] {
                Integer.parseInt(p[0]),  // día
                Integer.parseInt(p[1]),  // hora
                Integer.parseInt(p[2])   // minuto
        };
    }
}
