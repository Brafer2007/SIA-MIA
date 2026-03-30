package com.example.SIA.repository;

import com.example.SIA.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByNombreFicha(String nombreFicha);

    List<Tarea> findByInstructor_Id(Integer idInstructor);

    List<Tarea> findByNombreFichaAndInstructor_Id(String nombreFicha, Integer idInstructor);

    /**
     * Busca tareas cuyo nombreFicha contenga la ficha del aprendiz.
     * Cubre casos donde nombreFicha es "2996893" o "2996893 - 2996900".
     */
    @Query("SELECT t FROM Tarea t WHERE t.nombreFicha = :ficha OR t.nombreFicha LIKE CONCAT('%', :ficha, '%')")
    List<Tarea> findByNombreFichaContaining(@Param("ficha") String ficha);
}
