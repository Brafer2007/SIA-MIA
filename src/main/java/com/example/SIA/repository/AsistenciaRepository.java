package com.example.SIA.repository;

import com.example.SIA.entity.RegistroAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    List<RegistroAsistencia> findByInstructor_IdAndFecha(Integer idInstructor, LocalDate fecha);

    Optional<RegistroAsistencia> findByAprendiz_IdAprendizAndInstructor_IdAndFecha(
            Integer idAprendiz, Integer idInstructor, LocalDate fecha);

    @Query("SELECT r FROM RegistroAsistencia r WHERE r.instructor.id = :idInstructor " +
           "AND r.aprendiz.fichaFormacion = :ficha ORDER BY r.fecha DESC")
    List<RegistroAsistencia> findByInstructorAndFicha(
            @Param("idInstructor") Integer idInstructor,
            @Param("ficha") String ficha);

    /** Reporte semanal del instructor: registros entre dos fechas para una ficha */
    @Query("SELECT r FROM RegistroAsistencia r WHERE r.instructor.id = :idInstructor " +
           "AND r.fecha BETWEEN :desde AND :hasta ORDER BY r.fecha ASC, r.aprendiz.idAprendiz ASC")
    List<RegistroAsistencia> findByInstructorAndRango(
            @Param("idInstructor") Integer idInstructor,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /** Historial del aprendiz */
    @Query("SELECT r FROM RegistroAsistencia r WHERE r.aprendiz.idAprendiz = :idAprendiz " +
           "ORDER BY r.fecha DESC")
    List<RegistroAsistencia> findByAprendiz(@Param("idAprendiz") Integer idAprendiz);

    /** Historial del aprendiz en un rango */
    @Query("SELECT r FROM RegistroAsistencia r WHERE r.aprendiz.idAprendiz = :idAprendiz " +
           "AND r.fecha BETWEEN :desde AND :hasta ORDER BY r.fecha ASC")
    List<RegistroAsistencia> findByAprendizAndRango(
            @Param("idAprendiz") Integer idAprendiz,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
