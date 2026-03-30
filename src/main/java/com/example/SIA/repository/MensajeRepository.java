package com.example.SIA.repository;

import com.example.SIA.entity.MensajeGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MensajeRepository extends JpaRepository<MensajeGrupo, Long> {
    List<MensajeGrupo> findByFichaOrderByFechaEnvioAsc(String ficha);
    List<MensajeGrupo> findBySalaOrderByFechaEnvioAsc(String sala);
    
    // Buscar mensajes donde la sala contiene el patrón (ej: ficha% + instructor)
    @Query("SELECT m FROM MensajeGrupo m WHERE m.sala LIKE :salaPattern ORDER BY m.fechaEnvio ASC")
    List<MensajeGrupo> findBySalaPattern(@Param("salaPattern") String salaPattern);
}
