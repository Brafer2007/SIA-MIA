package com.example.SIA.repository;

import com.example.SIA.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {
    Optional<Perfil> findByNombrePerfil(String nombrePerfil);
}
