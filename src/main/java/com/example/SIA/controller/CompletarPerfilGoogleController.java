package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Formulario para completar los datos de un usuario que se registró por primera
 * vez usando Google OAuth2.
 *
 * Pide: nombres, apellidos, número de documento y nombre de usuario (opcional).
 * El correo ya viene del token de Google y no se puede cambiar aquí.
 * No pide contraseña porque la cuenta solo usará Google para autenticarse.
 */
@Controller
@RequestMapping("/completar-perfil-google")
public class CompletarPerfilGoogleController {

    private final UsuarioRepository usuarioRepository;

    public CompletarPerfilGoogleController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Si ya tiene datos completos no debería estar aquí
        if (usuario.getNoDocumento() != null
                && !usuario.getNoDocumento().startsWith("GOOGLE_PENDIENTE")) {
            return "redirect:/dashboard/invitado";
        }

        model.addAttribute("usuario", usuario);
        return "completar-perfil-google";
    }

    @PostMapping
    public String guardar(
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String noDocumento,
            @RequestParam(required = false) String nombreUsuario,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // ─── Validar nombres ────────────────────────────────────
        if (nombres == null || nombres.trim().length() < 2) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "El campo Nombres debe tener al menos 2 caracteres.");
            return "completar-perfil-google";
        }
        if (apellidos == null || apellidos.trim().length() < 2) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "El campo Apellidos debe tener al menos 2 caracteres.");
            return "completar-perfil-google";
        }

        // ─── Validar documento ──────────────────────────────────
        String docLimpio = noDocumento == null ? "" : noDocumento.trim();
        if (docLimpio.length() < 5 || !docLimpio.matches("\\d+")) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "El número de documento debe tener al menos 5 dígitos numéricos.");
            return "completar-perfil-google";
        }

        // Verificar que el documento no esté en uso por otro usuario
        if (usuarioRepository.existsByNoDocumento(docLimpio)) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "Ese número de documento ya está registrado en el sistema.");
            return "completar-perfil-google";
        }

        // ─── Validar nombre de usuario (opcional) ───────────────
        if (nombreUsuario != null && !nombreUsuario.trim().isBlank()) {
            String userLimpio = nombreUsuario.trim();
            if (userLimpio.length() < 4 || !userLimpio.matches("[a-zA-Z0-9.]+")) {
                model.addAttribute("usuario", usuario);
                model.addAttribute("error", "El nombre de usuario solo puede contener letras, números y puntos. Mínimo 4 caracteres.");
                return "completar-perfil-google";
            }
            // Verificar que no esté en uso por otro
            if (usuarioRepository.existsByNombreUsuario(userLimpio)
                    && !userLimpio.equalsIgnoreCase(usuario.getNombreUsuario())) {
                model.addAttribute("usuario", usuario);
                model.addAttribute("error", "Ese nombre de usuario ya está en uso. Elige otro.");
                return "completar-perfil-google";
            }
            usuario.setNombreUsuario(userLimpio);
        }

        // ─── Aplicar cambios ────────────────────────────────────
        usuario.setNombres(nombres.trim());
        usuario.setApellidos(apellidos.trim());
        usuario.setNoDocumento(docLimpio);

        usuarioRepository.save(usuario);

        // Refrescar sesión
        Usuario actualizado = usuarioRepository.findById(usuario.getIdUsuario()).orElse(usuario);
        session.setAttribute("usuario", actualizado);

        ra.addFlashAttribute("exito", "¡Bienvenido! Tu cuenta ha sido configurada correctamente.");
        return "redirect:/dashboard/invitado";
    }
}
