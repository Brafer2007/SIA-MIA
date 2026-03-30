package com.example.SIA.repository;

import com.example.SIA.entity.Programacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgramacionRepository extends JpaRepository<Programacion, Long> {

    // Buscar por nombres del usuario asociado al instructor
    List<Programacion> findByInstructorUsuarioNombres(String nombres);

    // Buscar por apellidos del usuario asociado al instructor
    List<Programacion> findByInstructorUsuarioApellidos(String apellidos);

    // Buscar por nombre completo (nombres + apellidos)
    @Query("SELECT p FROM Programacion p " +
            "WHERE UPPER(CONCAT(p.instructor.usuario.nombres, ' ', p.instructor.usuario.apellidos)) = UPPER(:nombreCompleto)")
    List<Programacion> findByNombreCompleto(@Param("nombreCompleto") String nombreCompleto);

    // Buscar todas las programaciones de un instructor por su ID
    List<Programacion> findByInstructorId(Long idInstructor);

    // Buscar programaciones por nombre de ficha (usando LIKE para detectar si la
    // ficha está dentro de un rango o lista)
    @Query("SELECT p FROM Programacion p WHERE p.nombreFicha LIKE %:nombreFicha%")
    List<Programacion> findByNombreFicha(@Param("nombreFicha") String nombreFicha);
}