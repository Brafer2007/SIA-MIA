
package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/completar-perfil")
public class CompletarPerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String mostrar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Si ya tiene documento real, saltar al dashboard
        if (usuario.getNoDocumento() != null && !usuario.getNoDocumento().startsWith("PENDIENTE")) {
            return "redirect:/dashboard/instructor";
        }

        model.addAttribute("usuario", usuario);
        return "completar-perfil";
    }

    @PostMapping
    public String guardar(@RequestParam String noDocumento,
                          @RequestParam(required = false) String nombreUsuario,
                          HttpSession session,
                          Model model,
                          RedirectAttributes ra) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Validar documento
        if (noDocumento == null || noDocumento.trim().length() < 5 || !noDocumento.trim().matches("\\d+")) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "El número de documento no es válido. Debe tener al menos 5 dígitos.");
            return "completar-perfil";
        }

        // Verificar que el documento no esté en uso por otro usuario
        String docLimpio = noDocumento.trim();
        boolean docExiste = usuarioRepository.existsByNoDocumento(docLimpio);
        if (docExiste) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "Ese número de documento ya está registrado en el sistema.");
            return "completar-perfil";
        }

        // Actualizar documento
        usuario.setNoDocumento(docLimpio);

        // Actualizar nombre de usuario si se proporcionó uno nuevo
        if (nombreUsuario != null && !nombreUsuario.trim().isEmpty()) {
            String nuevoUsername = nombreUsuario.trim();
            if (nuevoUsername.length() >= 4 && nuevoUsername.matches("[a-zA-Z0-9.]+")) {
                // Verificar que no esté en uso
                boolean usernameExiste = usuarioRepository.existsByNombreUsuario(nuevoUsername);
                if (usernameExiste && !nuevoUsername.equals(usuario.getNombreUsuario())) {
                    model.addAttribute("usuario", usuario);
                    model.addAttribute("error", "Ese nombre de usuario ya está en uso. Elige otro.");
                    return "completar-perfil";
                }
                usuario.setNombreUsuario(nuevoUsername);
            }
        }

        // Guardar en BD
        usuarioRepository.save(usuario);

        // Actualizar sesión con datos actualizados
        Usuario actualizado = usuarioRepository.findById(usuario.getIdUsuario()).orElse(usuario);
        session.setAttribute("usuario", actualizado);

        ra.addFlashAttribute("exito", "¡Perfil completado correctamente!");
        return "redirect:/dashboard/instructor";
    }
}
