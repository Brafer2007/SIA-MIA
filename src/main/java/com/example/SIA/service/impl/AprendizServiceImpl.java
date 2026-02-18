package com.example.SIA.service.impl;

import com.example.SIA.entity.Aprendiz;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.service.AprendizService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AprendizServiceImpl implements AprendizService {

    private final AprendizRepository aprendizRepository;

    public AprendizServiceImpl(AprendizRepository aprendizRepository) {
        this.aprendizRepository = aprendizRepository;
    }

    @Override
    public List<Aprendiz> findByUsuarioId(Integer idUsuario) {
        return aprendizRepository.findByUsuario_IdUsuario(idUsuario);
    }

    @Override
    public Aprendiz actualizarAprendiz(Aprendiz aprendiz) {
        return aprendizRepository.save(aprendiz);
    }
}
