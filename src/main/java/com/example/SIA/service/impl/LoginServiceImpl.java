package com.example.SIA.service.impl;

import com.example.SIA.dto.LoginRequest;
import com.example.SIA.dto.LoginResponse;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.LoginService;
import com.example.SIA.exception.LoginException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public LoginServiceImpl(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request.getUsuario() == null || request.getUsuario().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            throw new LoginException("Usuario y contraseña son obligatorios");
        }

        Usuario usuario = usuarioRepo.findByNombreUsuario(request.getUsuario())
                .orElseThrow(() -> new LoginException("Usuario no encontrado"));

        if (usuario.getEstado() != 1) {
            throw new LoginException("Este usuario está inactivo. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassUsuario())) {
            throw new LoginException("Contraseña incorrecta");
        }

        if (usuario.getPerfil() == null) {
            throw new LoginException("El usuario no tiene un perfil asignado. Contacte al administrador.");
        }

        Integer idPerfil = usuario.getPerfil().getIdPerfil();
        String redirect;

        switch (idPerfil) {
            case 1 -> redirect = "/dashboard/aprendiz";
            case 2 -> redirect = "/dashboard/admin";
            case 3 -> redirect = "/dashboard/instructor";
            case 4 -> redirect = "/dashboard/administrativo";
            default -> redirect = "/login";
        }

        return new LoginResponse(
                usuario.getIdUsuario(),
                idPerfil,
                usuario.getNombreUsuario(),
                usuario.getCorreo(),
                usuario.getPerfil().getNombrePerfil(),
                redirect
        );
    }

    @Override
    public Usuario obtenerUsuarioPorId(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new LoginException("Usuario no encontrado por ID"));
    }

    @Override
    public void logout() {
        // Si usas sesiones manuales, puedes invalidarlas desde el controlador
    }
}