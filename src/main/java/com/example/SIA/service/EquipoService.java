package com.example.SIA.service;

import com.example.SIA.entity.Equipo;
import com.example.SIA.dto.EquipoDTO;
import java.util.List;

public interface EquipoService {
    // EquipoService.java
Equipo registrarEquipo(EquipoDTO equipoDTO);

    List<Equipo> listarPorUsuario(Integer idUsuario);
    boolean eliminarEquipo(Integer idEquipo);
    void desactivarEquipo(Integer idEquipo, Integer idUsuario);
    Equipo buscarPorIdYUsuario(Integer idEquipo, Integer idUsuario);
    Equipo actualizarEquipo(EquipoDTO equipoDTO); // Ahora acepta DTO
}
