package com.example.SIA.repository;

import com.example.SIA.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    /** Solo para el admin (sin destinatario específico) */
    List<Notificacion> findByLeidaFalseAndDestinatarioIdIsNull();

    /** Para un usuario específico (no leídas) */
    List<Notificacion> findByDestinatarioIdAndLeidaFalseOrderByFechaDesc(Integer destinatarioId);

    /** Para un usuario específico (todas, máx 50 más recientes) */
    List<Notificacion> findTop50ByDestinatarioIdOrderByFechaDesc(Integer destinatarioId);

    /** Para el admin: últimas 50 sin destinatario (sistema) */
    List<Notificacion> findTop50ByDestinatarioIdIsNullOrderByFechaDesc();

    /** Marcar como leídas todas las de un destinatario */
    @Modifying
    @Transactional
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.destinatarioId = :id AND n.leida = false")
    void marcarLeidasPorDestinatario(@Param("id") Integer destinatarioId);

    /** Compatibilidad legacy (admin) */
    List<Notificacion> findByLeidaFalse();
}
