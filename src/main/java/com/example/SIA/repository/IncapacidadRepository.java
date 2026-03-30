package com.example.SIA.repository;

import com.example.SIA.entity.Incapacidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncapacidadRepository extends JpaRepository<Incapacidad, Long> {

    List<Incapacidad> findByAprendiz_IdAprendizOrderByFechaSubidaDesc(Integer idAprendiz);

    /** Incapacidades de aprendices de una ficha (para el instructor) */
    @Query("SELECT i FROM Incapacidad i WHERE i.aprendiz.fichaFormacion = :ficha " +
           "ORDER BY i.estado ASC, i.fechaSubida DESC")
    List<Incapacidad> findByFicha(@Param("ficha") String ficha);

    /** Incapacidades de aprendices cuya ficha está contenida en el nombreFicha */
    @Query("SELECT i FROM Incapacidad i WHERE :nombreFicha LIKE CONCAT('%', i.aprendiz.fichaFormacion, '%') " +
           "ORDER BY i.estado ASC, i.fechaSubida DESC")
    List<Incapacidad> findByFichaContaining(@Param("nombreFicha") String nombreFicha);

    /** Todas las incapacidades de aprendices de una lista de fichas */
    @Query("SELECT i FROM Incapacidad i WHERE i.aprendiz.fichaFormacion IN :fichas " +
           "ORDER BY i.estado ASC, i.fechaSubida DESC")
    List<Incapacidad> findByFichas(@Param("fichas") java.util.List<String> fichas);

    /**
     * Todas las incapacidades de aprendices cuya fichaFormacion esté contenida
     * en alguna de las fichas del instructor (cubre rangos como "2996893 - 2996900").
     * Se hace con una subquery dinámica en Java porque JPQL no soporta LIKE con lista.
     */
    @Query("SELECT i FROM Incapacidad i ORDER BY i.estado ASC, i.fechaSubida DESC")
    List<Incapacidad> findAll(org.springframework.data.domain.Sort sort);
}
