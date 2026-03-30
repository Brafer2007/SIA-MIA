package com.example.SIA.service.impl;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UsuarioRepository usuarioRepo;

    public AdminServiceImpl(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    public List<Usuario> getAllAdmin() {
        // Filtra usuarios con perfil ID = 2 (Administrador)
        return usuarioRepo.findAll().stream()
                .filter(u -> u.getPerfil() != null && u.getPerfil().getIdPerfil() == 2)
                .collect(Collectors.toList());
    }
}