package com.example.SIA.repository;

import com.example.SIA.entity.SolicitudAmbiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudAmbienteRepository extends JpaRepository<SolicitudAmbiente, Long> {
    List<SolicitudAmbiente> findByEstadoOrderByFechaSolicitudDesc(String estado);

    List<SolicitudAmbiente> findByInstructor_IdOrderByFechaSolicitudDesc(Integer idInstructor);
}
