package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints REST para acciones de perfil de cualquier usuario autenticado:
 *  - Cambiar contraseña
 *  - Subir/cambiar foto de perfil
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilUsuarioController {

    private static final String UPLOADS_DIR = "uploads/fotos/";
    private static final long MAX_FOTO_BYTES = 3 * 1024 * 1024L; // 3 MB

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilUsuarioController(UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Cambiar contraseña ───────────────────────────────────────────────────

    @PostMapping("/cambiar-password")
    public ResponseEntity<Map<String, Object>> cambiarPassword(
            @RequestParam String passwordActual,
            @RequestParam String passwordNueva,
            @RequestParam String passwordConfirm,
            HttpSession session) {

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElse(null);
        if (usuario == null) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));

        // Usuarios que solo tienen Google no tienen contraseña
        if (usuario.getPassUsuario() == null || usuario.getPassUsuario().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Tu cuenta solo usa Google para autenticarse. No tienes contraseña configurada."));
        }

        if (!passwordEncoder.matches(passwordActual, usuario.getPassUsuario())) {
            return ResponseEntity.badRequest().body(Map.of("error", "La contraseña actual es incorrecta."));
        }

        if (passwordNueva == null || passwordNueva.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña debe tener al menos 6 caracteres."));
        }

        if (!passwordNueva.equals(passwordConfirm)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas nuevas no coinciden."));
        }

        if (passwordEncoder.matches(passwordNueva, usuario.getPassUsuario())) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña no puede ser igual a la actual."));
        }

        usuario.setPassUsuario(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        session.setAttribute("usuario", usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }

    // ── Foto de perfil ───────────────────────────────────────────────────────

    @PostMapping("/foto")
    public ResponseEntity<Map<String, Object>> subirFoto(
            @RequestParam MultipartFile foto,
            HttpSession session) throws IOException {

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        if (foto == null || foto.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se recibió ningún archivo."));
        }

        String contentType = foto.getContentType() != null ? foto.getContentType().toLowerCase() : "";
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten imágenes (JPG, PNG, WEBP)."));
        }

        if (foto.getSize() > MAX_FOTO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen no puede superar los 3 MB."));
        }

        // Guardar archivo
        Path dir = Paths.get(UPLOADS_DIR);
        Files.createDirectories(dir);

        String ext = obtenerExtension(foto.getOriginalFilename());
        String nombreArchivo = "usuario_" + idUsuario + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path destino = dir.resolve(nombreArchivo);
        Files.copy(foto.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        // Eliminar foto anterior si existe
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));

        if (usuario.getFotoPerfil() != null) {
            try { Files.deleteIfExists(Paths.get(usuario.getFotoPerfil())); } catch (Exception ignored) {}
        }

        String rutaRelativa = UPLOADS_DIR + nombreArchivo;
        usuario.setFotoPerfil(rutaRelativa);
        usuarioRepository.save(usuario);
        session.setAttribute("usuario", usuario);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Foto actualizada correctamente.",
                "url", "/" + rutaRelativa
        ));
    }

    @GetMapping("/foto-url")
    public ResponseEntity<Map<String, Object>> fotoUrl(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        return usuarioRepository.findById(idUsuario)
                .map(u -> u.getFotoPerfil() != null
                        ? ResponseEntity.ok(Map.<String,Object>of("url", "/" + u.getFotoPerfil()))
                        : ResponseEntity.ok(Map.<String,Object>of()))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    @DeleteMapping("/foto")
    public ResponseEntity<Map<String, Object>> eliminarFoto(HttpSession session) {        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));

        if (usuario.getFotoPerfil() != null) {
            try { Files.deleteIfExists(Paths.get(usuario.getFotoPerfil())); } catch (Exception ignored) {}
            usuario.setFotoPerfil(null);
            usuarioRepository.save(usuario);
            session.setAttribute("usuario", usuario);
        }

        return ResponseEntity.ok(Map.of("mensaje", "Foto eliminada."));
    }

    private String obtenerExtension(String nombre) {
        if (nombre == null || !nombre.contains(".")) return ".jpg";
        return nombre.substring(nombre.lastIndexOf(".")).toLowerCase();
    }
}
