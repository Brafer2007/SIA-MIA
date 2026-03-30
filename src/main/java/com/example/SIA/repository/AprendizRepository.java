package com.example.SIA.repository;

import com.example.SIA.entity.Aprendiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AprendizRepository extends JpaRepository<Aprendiz, Integer> {

    List<Aprendiz> findByUsuario_IdUsuario(Integer idUsuario);

    List<Aprendiz> findByFichaFormacion(String fichaFormacion);

    /**
     * Busca aprendices cuya fichaFormacion esté contenida en el nombreFicha de la tarea.
     * Cubre el caso inverso: tarea.nombreFicha="2996893 - 2996900", aprendiz.fichaFormacion="2996893"
     */
    @Query("SELECT a FROM Aprendiz a WHERE :nombreFicha LIKE CONCAT('%', a.fichaFormacion, '%')")
    List<Aprendiz> findByFichaContainedIn(@Param("nombreFicha") String nombreFicha);

    @Query("SELECT a.programaFormacion, COUNT(a) FROM Aprendiz a GROUP BY a.programaFormacion")
    List<Object[]> countByProgramaFormacion();
}
