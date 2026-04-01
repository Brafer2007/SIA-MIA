package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/recuperar-password")
public class RecuperarPasswordController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Value("${spring.mail.username}")
    private String remitente;

    // Token -> {correo, expiracion}
    private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private static final int EXPIRACION_MINUTOS = 30;

    record TokenInfo(String correo, LocalDateTime expiracion) {}

    // ── GET: mostrar formulario de solicitud ──────────────────────────────────
    @GetMapping
    public String mostrarFormulario() {
        return "recuperar-password";
    }

    // ── POST: procesar solicitud y enviar email ───────────────────────────────
    @PostMapping
    public String procesarSolicitud(@RequestParam String correo,
                                    HttpServletRequest request,
                                    RedirectAttributes ra) {
        Optional<Usuario> opt = usuarioRepository.findByCorreo(correo.trim().toLowerCase());

        // Siempre mostrar el mismo mensaje para no revelar si el correo existe
        String mensajeOk = "Si el correo está registrado, recibirás un enlace en los próximos minutos.";

        if (opt.isPresent()) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, new TokenInfo(correo.trim().toLowerCase(),
                    LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS)));

            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort() : "");
            String enlace = baseUrl + "/recuperar-password/nueva?token=" + token;

            String cuerpo = "Hola " + opt.get().getNombres() + ",\n\n"
                    + "Recibimos una solicitud para restablecer tu contraseña en SIA.\n\n"
                    + "Haz clic en el siguiente enlace (válido por " + EXPIRACION_MINUTOS + " minutos):\n"
                    + enlace + "\n\n"
                    + "Si no solicitaste esto, ignora este mensaje.\n\n"
                    + "— Equipo SIA SENA";

            emailService.enviarCorreoIndividual(correo, "Recuperación de contraseña - SIA", cuerpo, remitente);
        }

        ra.addFlashAttribute("mensaje", mensajeOk);
        return "redirect:/recuperar-password";
    }

    // ── GET: formulario nueva contraseña ─────────────────────────────────────
    @GetMapping("/nueva")
    public String mostrarNuevaPassword(@RequestParam String token, Model model) {
        TokenInfo info = tokens.get(token);
        if (info == null || LocalDateTime.now().isAfter(info.expiracion())) {
            model.addAttribute("error", "El enlace ha expirado o no es válido. Solicita uno nuevo.");
            return "recuperar-password";
        }
        model.addAttribute("token", token);
        return "nueva-password";
    }

    // ── POST: guardar nueva contraseña ────────────────────────────────────────
    @PostMapping("/nueva")
    public String guardarNuevaPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes ra) {
        TokenInfo info = tokens.get(token);
        if (info == null || LocalDateTime.now().isAfter(info.expiracion())) {
            ra.addFlashAttribute("error", "El enlace ha expirado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            ra.addFlashAttribute("token", token);
            return "redirect:/recuperar-password/nueva?token=" + token;
        }

        if (password.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            ra.addFlashAttribute("token", token);
            return "redirect:/recuperar-password/nueva?token=" + token;
        }

        Optional<Usuario> opt = usuarioRepository.findByCorreo(info.correo());
        if (opt.isPresent()) {
            Usuario u = opt.get();
            u.setPassUsuario(password);
            usuarioRepository.save(u);
            tokens.remove(token);
            ra.addFlashAttribute("exito", "¡Contraseña actualizada! Ya puedes iniciar sesión.");
            return "redirect:/login";
        }

        ra.addFlashAttribute("error", "No se encontró el usuario. Intenta de nuevo.");
        return "redirect:/recuperar-password";
    }
}
