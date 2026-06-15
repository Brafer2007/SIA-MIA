package com.example.SIA.controller;

import com.example.SIA.entity.TokenRecuperacion;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.TokenRecuperacionRepository;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/recuperar-password")
public class RecuperarPasswordController {

    private static final Logger log = LoggerFactory.getLogger(RecuperarPasswordController.class);
    private static final int EXPIRACION_MINUTOS = 30;
    private static final String REMITENTE = "SiaNotificacionesNoReply@gmail.com";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── GET: Mostrar formulario de solicitud ───────────────────────────────
    @GetMapping
    public String mostrarFormulario() {
        return "recuperar-password";
    }

    // ─── POST: Usuario solicita recuperación ────────────────────────────────
    @PostMapping
    public String procesarSolicitud(@RequestParam String correo,
                                    HttpServletRequest request,
                                    RedirectAttributes ra) {
        try {
            String correoNorm = correo.trim().toLowerCase();
            log.info("[Recuperación] Solicitud para: {}", correoNorm);

            Optional<Usuario> opt = usuarioRepository.findByCorreoIgnoreCase(correoNorm);

            if (opt.isPresent()) {
                // Invalidar tokens anteriores del mismo correo
                tokenRepository.deleteByCorreo(correoNorm);

                // Limpiar tokens vencidos de otros usuarios
                tokenRepository.limpiarTokensVencidos(LocalDateTime.now());

                // Generar token único y guardarlo en BD
                String token = UUID.randomUUID().toString().replace("-", "");
                TokenRecuperacion tokenEntity = new TokenRecuperacion(
                        token,
                        correoNorm,
                        LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS)
                );
                tokenRepository.save(tokenEntity);

                // Construir URL respetando proxy (Render, Nginx, etc.)
                String scheme = Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
                        .orElse(request.getScheme());
                String host = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                        .orElse(request.getServerName());
                // Agregar puerto solo si es local y no estándar
                String port = "";
                int serverPort = request.getServerPort();
                if (host.equals("localhost") || host.equals("127.0.0.1")) {
                    if (serverPort != 80 && serverPort != 443) {
                        port = ":" + serverPort;
                    }
                }
                String enlace = scheme + "://" + host + port + "/recuperar-password/nueva?token=" + token;

                String nombre = opt.get().getNombres();
                String asunto = "Recuperación de contraseña - SIA";
                String cuerpo = "Hola " + nombre + ",\n\n"
                        + "Recibimos una solicitud para restablecer tu contraseña en el sistema SIA.\n\n"
                        + "Haz clic en el siguiente enlace para crear una nueva contraseña.\n"
                        + "Este enlace es válido por " + EXPIRACION_MINUTOS + " minutos:\n\n"
                        + enlace + "\n\n"
                        + "Si no solicitaste este cambio, ignora este mensaje. Tu contraseña actual seguirá siendo la misma.\n\n"
                        + "— Sistema SIA\n"
                        + "Este es un correo automático, por favor no respondas a este mensaje.";

                boolean enviado = emailService.enviarCorreoIndividual(correoNorm, asunto, cuerpo, REMITENTE);
                log.info("[Recuperación] Correo enviado a {}: {}", correoNorm, enviado);
            } else {
                log.info("[Recuperación] Correo no encontrado: {}", correoNorm);
            }

        } catch (Exception e) {
            log.error("[Recuperación] Error: {}", e.getMessage(), e);
        }

        // Siempre el mismo mensaje (no revelar si el correo existe)
        ra.addFlashAttribute("mensaje",
                "Si el correo está registrado, recibirás un enlace en los próximos minutos. Revisa también tu carpeta de spam.");
        return "redirect:/login";
    }

    // ─── GET: Mostrar formulario de nueva contraseña ────────────────────────
    @GetMapping("/nueva")
    public String mostrarNuevaPassword(@RequestParam String token, Model model) {
        Optional<TokenRecuperacion> opt = tokenRepository.findByToken(token);

        if (opt.isEmpty() || !opt.get().isValido()) {
            model.addAttribute("error", "El enlace ha expirado o no es válido. Solicita uno nuevo.");
            return "recuperar-password";
        }

        model.addAttribute("token", token);
        return "nueva-password";
    }

    // ─── POST: Guardar nueva contraseña e invalidar token ───────────────────
    @PostMapping("/nueva")
    public String guardarNuevaPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes ra) {
        Optional<TokenRecuperacion> opt = tokenRepository.findByToken(token);

        if (opt.isEmpty() || !opt.get().isValido()) {
            ra.addFlashAttribute("error", "El enlace ha expirado o ya fue utilizado. Solicita uno nuevo.");
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

        TokenRecuperacion tokenEntity = opt.get();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(tokenEntity.getCorreo());

        if (usuarioOpt.isPresent()) {
            // Actualizar contraseña
            Usuario u = usuarioOpt.get();
            u.setPassUsuario(passwordEncoder.encode(password));
            usuarioRepository.save(u);

            // Invalidar token (marcarlo como usado)
            tokenEntity.setUsado(true);
            tokenRepository.save(tokenEntity);

            log.info("[Recuperación] Contraseña actualizada para: {}", tokenEntity.getCorreo());
            ra.addFlashAttribute("exito", "¡Contraseña actualizada correctamente! Ya puedes iniciar sesión.");
            return "redirect:/login";
        }

        ra.addFlashAttribute("error", "No se encontró el usuario asociado. Intenta de nuevo.");
        return "redirect:/recuperar-password";
    }
}
