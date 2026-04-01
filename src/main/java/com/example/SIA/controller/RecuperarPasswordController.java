package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private static final Logger log = LoggerFactory.getLogger(RecuperarPasswordController.class);
    private static final int EXPIRACION_MINUTOS = 30;

    // token -> [correo, fechaExpiracion]
    private static final Map<String, Object[]> tokens = new ConcurrentHashMap<>();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String remitente;

    @GetMapping
    public String mostrarFormulario() {
        return "recuperar-password";
    }

    @PostMapping
    public String procesarSolicitud(@RequestParam String correo,
                                    HttpServletRequest request,
                                    RedirectAttributes ra) {
        try {
            String correoNorm = correo.trim().toLowerCase();
            Optional<Usuario> opt = usuarioRepository.findByCorreo(correoNorm);

            if (opt.isPresent()) {
                String token = UUID.randomUUID().toString();
                tokens.put(token, new Object[]{correoNorm, LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS)});

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

                emailService.enviarCorreoIndividual(correoNorm,
                        "Recuperación de contraseña - SIA", cuerpo, remitente);
            }
        } catch (Exception e) {
            log.error("Error al procesar recuperación de contraseña: {}", e.getMessage(), e);
        }

        ra.addFlashAttribute("mensaje",
                "Si el correo está registrado, recibirás un enlace en los próximos minutos.");
        return "redirect:/recuperar-password";
    }

    @GetMapping("/nueva")
    public String mostrarNuevaPassword(@RequestParam String token, Model model) {
        Object[] info = tokens.get(token);
        if (info == null || LocalDateTime.now().isAfter((LocalDateTime) info[1])) {
            model.addAttribute("error", "El enlace ha expirado o no es válido. Solicita uno nuevo.");
            return "recuperar-password";
        }
        model.addAttribute("token", token);
        return "nueva-password";
    }

    @PostMapping("/nueva")
    public String guardarNuevaPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes ra) {
        Object[] info = tokens.get(token);
        if (info == null || LocalDateTime.now().isAfter((LocalDateTime) info[1])) {
            ra.addFlashAttribute("error", "El enlace ha expirado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/recuperar-password/nueva?token=" + token;
        }

        if (password.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            return "redirect:/recuperar-password/nueva?token=" + token;
        }

        String correo = (String) info[0];
        Optional<Usuario> opt = usuarioRepository.findByCorreo(correo);
        if (opt.isPresent()) {
            Usuario u = opt.get();
            u.setPassUsuario(passwordEncoder.encode(password)); // ← BCrypt
            usuarioRepository.save(u);
            tokens.remove(token);
            ra.addFlashAttribute("exito", "¡Contraseña actualizada! Ya puedes iniciar sesión.");
            return "redirect:/login";
        }

        ra.addFlashAttribute("error", "No se encontró el usuario. Intenta de nuevo.");
        return "redirect:/recuperar-password";
    }
}
