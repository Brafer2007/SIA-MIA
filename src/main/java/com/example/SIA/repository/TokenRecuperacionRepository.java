
package com.example.SIA.repository;

import com.example.SIA.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.correo = :correo")
    void deleteByCorreo(String correo);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.expiracion < :ahora OR t.usado = true")
    void limpiarTokensVencidos(LocalDateTime ahora);
}
