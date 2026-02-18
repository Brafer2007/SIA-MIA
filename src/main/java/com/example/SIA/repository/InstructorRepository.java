package com.example.SIA.repository;

import com.example.SIA.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    // Buscar por nombres del usuario asociado
    Optional<Instructor> findByUsuarioNombres(String nombres);

    // Buscar por apellidos del usuario asociado
    Optional<Instructor> findByUsuarioApellidos(String apellidos);

    // Buscar por nombre completo
    @Query("SELECT i FROM Instructor i " +
           "WHERE UPPER(CONCAT(i.usuario.nombres, ' ', i.usuario.apellidos)) = UPPER(:nombreCompleto)")
    Optional<Instructor> findByNombreCompleto(@Param("nombreCompleto") String nombreCompleto);
}