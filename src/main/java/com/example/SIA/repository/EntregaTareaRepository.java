package com.example.SIA.repository;

import com.example.SIA.entity.EntregaTarea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Long> {

    Optional<EntregaTarea> findByTarea_IdAndAprendiz_IdAprendiz(Long idTarea, Integer idAprendiz);

    List<EntregaTarea> findByTarea_Id(Long idTarea);
}
