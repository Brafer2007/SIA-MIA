package com.example.SIA.repository;

import com.example.SIA.entity.Aprendiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AprendizRepository extends JpaRepository<Aprendiz, Integer> {

    List<Aprendiz> findByUsuario_IdUsuario(Integer idUsuario);

    @Query("SELECT a.programaFormacion, COUNT(a) FROM Aprendiz a GROUP BY a.programaFormacion")
    List<Object[]> countByProgramaFormacion();
}
