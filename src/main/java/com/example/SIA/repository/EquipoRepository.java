package com.example.SIA.repository;

import com.example.SIA.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Integer> {

    List<Equipo> findByUsuario_IdUsuarioAndEstado(Integer idUsuario, Integer estado);

    List<Equipo> findByUsuario_IdUsuario(Integer idUsuario);

    boolean existsByNumeroSerie(String numeroSerie);

    @Query("SELECT COUNT(e) FROM Equipo e")
    Long countTotalEquipos();

    @Query("SELECT e.tipoEquipo, COUNT(e) FROM Equipo e GROUP BY e.tipoEquipo")
    List<Object[]> countByTipoEquipo();
}
