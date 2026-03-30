package com.example.SIA.service;

import com.example.SIA.entity.Aprendiz;
import java.util.List;

public interface AprendizService {

    // Buscar todos los aprendices de un usuario
    List<Aprendiz> findByUsuarioId(Integer idUsuario);

    // Actualizar aprendiz
    Aprendiz actualizarAprendiz(Aprendiz aprendiz);
}
