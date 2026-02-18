package com.example.SIA.pattern.strategy;

import com.example.SIA.dto.FestivoDTO;
import com.example.SIA.entity.Programacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CONCRETE STRATEGY
 * Implementa la lógica específica para ajustar el calendario basándose en
 * los festivos de Colombia.
 */
@Component
public class AjusteColombiaStrategy implements AjusteCalendarioStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AjusteColombiaStrategy.class);

    @Override
    public void ajustar(List<Programacion> programacionLista, List<FestivoDTO> festivosDTO) {

        // 1. Convertir DTOs a Set<LocalDate> para búsqueda rápida
        Set<LocalDate> diasFestivos = new HashSet<>();
        if (festivosDTO != null) {
            for (FestivoDTO f : festivosDTO) {
                try {
                    diasFestivos.add(LocalDate.parse(f.getDate()));
                } catch (Exception e) {
                    logger.warn("No se pudo parsear fecha festivo: " + f.getDate());
                }
            }
        }

        // 2. Clases auxiliares y ordenamiento
        int anioActual = Year.now().getValue();

        class ProgramacionWrapper {
            Programacion prog;
            LocalDate fechaOriginal;

            public ProgramacionWrapper(Programacion p) {
                this.prog = p;
                this.fechaOriginal = parsearFechaEsp(p.getDia(), anioActual);
            }
        }

        List<ProgramacionWrapper> listaOrdenada = programacionLista.stream()
                .map(ProgramacionWrapper::new)
                .filter(w -> w.fechaOriginal != null)
                .sorted(Comparator.comparing(w -> w.fechaOriginal))
                .collect(Collectors.toList());

        // 3. Algoritmo de desplazamiento (Queue)
        if (!listaOrdenada.isEmpty()) {
            LocalDate cursorFecha = listaOrdenada.get(0).fechaOriginal;

            for (ProgramacionWrapper item : listaOrdenada) {
                if (item.fechaOriginal.isAfter(cursorFecha)) {
                    cursorFecha = item.fechaOriginal;
                }

                while (esDiaNoHabil(cursorFecha, diasFestivos)) {
                    logger.info("[Strategy] Saltando día no hábil: {}", cursorFecha);
                    cursorFecha = cursorFecha.plusDays(1);
                }

                String nuevaFechaTexto = formatearFechaEsp(cursorFecha);
                item.prog.setDia(nuevaFechaTexto);

                cursorFecha = cursorFecha.plusDays(1);
            }
        }
    }

    private boolean esDiaNoHabil(LocalDate fecha, Set<LocalDate> festivos) {
        return fecha.getDayOfWeek() == DayOfWeek.SUNDAY || festivos.contains(fecha);
    }

    // Métodos helper copiados para encapsular la lógica completa aquí
    private LocalDate parsearFechaEsp(String diaMes, int anio) {
        try {
            String[] partes = diaMes.trim().split(" ");
            if (partes.length < 2)
                return null;

            int dia = Integer.parseInt(partes[0]);
            String mesNombre = partes[1].toUpperCase();

            int mes = 1;
            if (mesNombre.contains("ENERO"))
                mes = 1;
            else if (mesNombre.contains("FEBRERO"))
                mes = 2;
            else if (mesNombre.contains("MARZO"))
                mes = 3;
            else if (mesNombre.contains("ABRIL"))
                mes = 4;
            else if (mesNombre.contains("MAYO"))
                mes = 5;
            else if (mesNombre.contains("JUNIO"))
                mes = 6;
            else if (mesNombre.contains("JULIO"))
                mes = 7;
            else if (mesNombre.contains("AGOSTO"))
                mes = 8;
            else if (mesNombre.contains("SEPTIEMBRE"))
                mes = 9;
            else if (mesNombre.contains("OCTUBRE"))
                mes = 10;
            else if (mesNombre.contains("NOVIEMBRE"))
                mes = 11;
            else if (mesNombre.contains("DICIEMBRE"))
                mes = 12;

            return LocalDate.of(anio, mes, dia);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatearFechaEsp(LocalDate fecha) {
        String[] meses = { "", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE",
                "OCTUBRE", "NOVIEMBRE", "DICIEMBRE" };
        return fecha.getDayOfMonth() + " " + meses[fecha.getMonthValue()];
    }
}
