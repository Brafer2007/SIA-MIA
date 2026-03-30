package com.example.SIA.repository;

import com.example.SIA.entity.RegistroAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {
    List<RegistroAcceso> findByUsuario_IdUsuarioOrderByFechaHoraDesc(Integer idUsuario);

    // Obtiene el registro más reciente de un usuario
    Optional<RegistroAcceso> findTopByUsuario_IdUsuarioOrderByFechaHoraDesc(Integer idUsuario);

    // Registros en un rango de fechas (para reporte diario)
    List<RegistroAcceso> findByFechaHoraBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);
}
