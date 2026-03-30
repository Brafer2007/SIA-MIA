package com.example.SIA.service.impl;

import com.example.SIA.entity.Perfil;
import com.example.SIA.repository.PerfilRepository;
import com.example.SIA.service.PerfilService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerfilServiceImpl implements PerfilService {

    private final PerfilRepository perfilRepository;

    public PerfilServiceImpl(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Override
    public List<Perfil> findAll() {
        return perfilRepository.findAll();
    }

    @Override
    public Perfil findById(Integer id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado con id: " + id));
    }
}
