package com.example.SIA.pattern.strategy;

import com.example.SIA.dto.FestivoDTO;
import com.example.SIA.entity.Programacion;
import java.util.List;

/**
 * STRATEGY PATTERN
 * Define una familia de algoritmos (estrategias de ajuste de calendario),
 * encapsula cada uno de ellos y los hace intercambiables.
 */
public interface AjusteCalendarioStrategy {
    void ajustar(List<Programacion> programacionLista, List<FestivoDTO> festivosDTO);
}
