package com.example.SIA.service;

import com.example.SIA.entity.Perfil;
import java.util.List;

public interface PerfilService {
    List<Perfil> findAll();
    Perfil findById(Integer id);
}
