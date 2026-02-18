package com.example.SIA.service;

import com.example.SIA.entity.MensajeGrupo;
import com.example.SIA.repository.MensajeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;

    public MensajeService(MensajeRepository mensajeRepository) {
        this.mensajeRepository = mensajeRepository;
    }

    public List<MensajeGrupo> obtenerMensajesPorFicha(String ficha) {
        return mensajeRepository.findByFichaOrderByFechaEnvioAsc(ficha);
    }

    public List<MensajeGrupo> obtenerMensajesPorSala(String sala) {
        return mensajeRepository.findBySalaOrderByFechaEnvioAsc(sala);
    }

    public List<MensajeGrupo> obtenerMensajesPorSalaPattern(String salaPattern) {
        return mensajeRepository.findBySalaPattern(salaPattern);
    }

    public MensajeGrupo guardarMensaje(MensajeGrupo mensaje) {
        return mensajeRepository.save(mensaje);
    }

    public List<MensajeGrupo> obtenerTodosMensajes() {
        return mensajeRepository.findAll();
    }

    public String construirSala(String fichaBase, Long instructorId) {
        if (fichaBase == null || instructorId == null) return "";
        // Formato: "fichaBase|instructorId"
        return fichaBase.trim() + "|" + instructorId;
    }
}
