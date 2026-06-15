package com.example.SIA.repository;

import com.example.SIA.entity.RespuestaEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RespuestaEncuestaRepository extends JpaRepository<RespuestaEncuesta, Long> {

    @Query("SELECT r.tipoInstitucion, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.tipoInstitucion")
    List<Object[]> countByTipoInstitucion();

    @Query("SELECT r.asistenciaDigital, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.asistenciaDigital")
    List<Object[]> countByAsistenciaDigital();

    @Query("SELECT r.controlAcceso, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.controlAcceso")
    List<Object[]> countByControlAcceso();

    @Query("SELECT r.comunicacion, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.comunicacion")
    List<Object[]> countByComunicacion();

    @Query("SELECT r.tareasDigital, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.tareasDigital")
    List<Object[]> countByTareasDigital();

    @Query("SELECT r.certificados, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.certificados")
    List<Object[]> countByCertificados();

    @Query("SELECT r.mayorFalencia, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.mayorFalencia")
    List<Object[]> countByMayorFalencia();

    @Query("SELECT r.usariaSia, COUNT(r) FROM RespuestaEncuesta r GROUP BY r.usariaSia")
    List<Object[]> countByUsariaSia();

    @Query("SELECT AVG(r.problemasAsistencia) FROM RespuestaEncuesta r WHERE r.problemasAsistencia IS NOT NULL")
    Double avgProblemasAsistencia();

    @Query("SELECT AVG(r.facilidadTareas) FROM RespuestaEncuesta r WHERE r.facilidadTareas IS NOT NULL")
    Double avgFacilidadTareas();

    @Query("SELECT AVG(r.satisfaccionAdmin) FROM RespuestaEncuesta r WHERE r.satisfaccionAdmin IS NOT NULL")
    Double avgSatisfaccionAdmin();

    @Query("SELECT DISTINCT r.nombreInstitucion FROM RespuestaEncuesta r WHERE r.nombreInstitucion IS NOT NULL AND LOWER(r.nombreInstitucion) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY r.nombreInstitucion")
    List<String> findInstitucionesByQuery(@org.springframework.data.repository.query.Param("q") String q);
}
