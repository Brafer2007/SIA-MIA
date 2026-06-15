package com.example.SIA.repository;

import com.example.SIA.entity.TokenVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {

    Optional<TokenVerificacion> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenVerificacion t WHERE t.correo = :correo")
    void deleteByCorreo(@Param("correo") String correo);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenVerificacion t WHERE t.expiracion < :ahora")
    void limpiarExpirados(@Param("ahora") LocalDateTime ahora);
}
