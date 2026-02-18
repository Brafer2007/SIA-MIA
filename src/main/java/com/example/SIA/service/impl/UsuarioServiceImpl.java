package com.example.SIA.service.impl;

import com.example.SIA.dto.*;
import com.example.SIA.entity.Perfil;
import com.example.SIA.entity.Usuario;
import com.example.SIA.observer.EventoUsuarioRegistrado;
import com.example.SIA.observer.SistemaEventos;
import com.example.SIA.repository.PerfilRepository;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.util.ExcelExporter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// 🔴 PATRÓN STRATEGY - IMPLEMENTACIÓN CONCRETA
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioServiceImpl(UsuarioRepository usuarioRepo, PerfilRepository perfilRepo) {
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
    }

    // 📌 Listar usuarios como DTO
    @Override
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 📌 Crear usuario desde panel admin
    @Override
    public UsuarioResponse crearUsuario(UsuarioRequest req) {
        Usuario u = new Usuario();
        u.setNombreUsuario(req.getNombreUsuario());
        u.setNombres(req.getNombres());
        u.setApellidos(req.getApellidos());
        u.setCorreo(req.getCorreo());
        u.setNoDocumento(req.getDocumento());
        u.setPassUsuario(encoder.encode(req.getClave()));
        u.setEstado(1);

        Perfil perfil = perfilRepo.findById(req.getPerfil())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
        u.setPerfil(perfil);

        return mapToResponse(usuarioRepo.save(u));
    }

    // 📌 Actualizar usuario existente
    @Override
    public UsuarioResponse actualizarUsuario(UsuarioUpdateRequest req) {
        Usuario u = usuarioRepo.findById(req.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (req.getNombreUsuario() != null) u.setNombreUsuario(req.getNombreUsuario());
        if (req.getNombres() != null) u.setNombres(req.getNombres());
        if (req.getApellidos() != null) u.setApellidos(req.getApellidos());
        if (req.getCorreo() != null) u.setCorreo(req.getCorreo());
        if (req.getNoDocumento() != null) u.setNoDocumento(req.getNoDocumento());

        if (req.getClave() != null && !req.getClave().isBlank()) {
            u.setPassUsuario(encoder.encode(req.getClave()));
        }

        if (req.getIdPerfil() != null) {
            Perfil perfil = perfilRepo.findById(req.getIdPerfil())
                    .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));
            u.setPerfil(perfil);
        }

        return mapToResponse(usuarioRepo.save(u));
    }

    // 📌 Registro desde formulario público
    // ✅ Aquí disparamos el evento del OBSERVER
    @Override
    public UsuarioResponse registrarUsuario(UsuarioRegistroRequest req) {

        if (!req.getPassUsuario().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        if (usuarioRepo.existsByCorreo(req.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        if (usuarioRepo.existsByNoDocumento(req.getNoDocumento())) {
            throw new RuntimeException("El documento ya está registrado");
        }

        Usuario u = new Usuario();
        u.setNombreUsuario(req.getNombreUsuario());
        u.setNombres(req.getNombres());
        u.setApellidos(req.getApellidos());
        u.setCorreo(req.getCorreo());
        u.setNoDocumento(req.getNoDocumento());
        u.setPassUsuario(encoder.encode(req.getPassUsuario()));
        u.setEstado(1);

        // Perfil aprendiz por defecto
        Perfil perfil = perfilRepo.findById(1)
                .orElseThrow(() -> new RuntimeException("Perfil aprendiz no encontrado"));
        u.setPerfil(perfil);

        Usuario guardado = usuarioRepo.save(u);

        // 🔔 Notificar al administrador (Observer GOF)
        SistemaEventos.emitir(new EventoUsuarioRegistrado(guardado));

        return mapToResponse(guardado);
    }

    // 📌 Desactivar usuario
    @Override
    public boolean desactivar(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario).map(u -> {
            u.setEstado(0);
            usuarioRepo.save(u);
            return true;
        }).orElse(false);
    }

    // 📌 Activar usuario
    @Override
    public boolean activar(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario).map(u -> {
            u.setEstado(1);
            usuarioRepo.save(u);
            return true;
        }).orElse(false);
    }

    // 📌 Exportar usuarios a Excel
    @Override
    public byte[] exportarExcel() {
        return ExcelExporter.exportarUsuarios(listarUsuarios());
    }

    // 📌 Buscar entidad por ID
    @Override
    public Usuario findById(Integer idUsuario) {
        return usuarioRepo.findById(idUsuario).orElse(null);
    }

    // 📌 Listar entidades completas (sin DTO)
    @Override
    public List<Usuario> findAll() {
        return usuarioRepo.findAll();
    }

    // 🔄 Convertir entidad a DTO
    private UsuarioResponse mapToResponse(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(),
                u.getNombreUsuario(),
                u.getNombres(),
                u.getApellidos(),
                u.getCorreo(),
                u.getNoDocumento(),
                u.getPerfil() != null ? u.getPerfil().getNombrePerfil() : null,
                u.getEstado()
        );
    }

    @Override
    public Usuario actualizar(Usuario usuario) {
        if (usuario.getIdUsuario() != null) {
            Usuario original = usuarioRepo.findById(usuario.getIdUsuario()).orElse(null);
            if (original != null) {
                // Preservar por defecto los campos que no deben cambiar
                if (usuario.getPerfil() == null) {
                    usuario.setPerfil(original.getPerfil());
                }
                // Preservar estado actual - NO debe cambiar al editar perfil del aprendiz
                if (usuario.getEstado() == null || usuario.getEstado() == 0) {
                    usuario.setEstado(original.getEstado());
                }
            }
        }
        return usuarioRepo.save(usuario);
    }
}