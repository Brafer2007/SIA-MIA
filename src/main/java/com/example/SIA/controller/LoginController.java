package com.example.SIA.controller;

import com.example.SIA.dto.LoginRequest;
import com.example.SIA.dto.LoginResponse;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.LoginService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/login")
public class LoginController {

    private static final int MAX_INTENTOS  = 5;
    private static final int BLOQUEO_MIN   = 15;

    private final LoginService     loginService;
    private final UsuarioRepository usuarioRepository;

    public LoginController(LoginService loginService, UsuarioRepository usuarioRepository) {
        this.loginService      = loginService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrarLogin(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String oauth2Error,
            Model model) {
        if (error != null && !error.isBlank()) {
            model.addAttribute("error", error);
        } else if (oauth2Error != null) {
            String msg = switch (oauth2Error) {
                case "registro_fallido"  -> "Hubo un problema al registrar tu cuenta con Google. Intenta de nuevo.";
                case "user_not_registered" -> "El correo de Google no está registrado en el sistema.";
                case "user_inactive"     -> "Tu cuenta está inactiva. Contacta al administrador.";
                case "email_not_found"   -> "No se pudo obtener el correo desde Google. Intenta de nuevo.";
                case "server_error"      -> "Error interno al procesar Google. Revisa los logs e intenta de nuevo.";
                default                  -> "No se pudo iniciar sesión con Google. Intenta de nuevo.";
            };
            model.addAttribute("error", msg);
        }
        return "login";
    }

    @PostMapping
    public String acceder(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        // ── Rate limiting: verificar bloqueo por usuario ─────────────────
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombreUsuario(email);
        if (usuarioOpt.isEmpty()) {
            // También buscar por correo
            usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(email);
        }
        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            if (u.getBloqueadoHasta() != null && LocalDateTime.now().isBefore(u.getBloqueadoHasta())) {
                long minutosRestantes = java.time.Duration.between(
                        LocalDateTime.now(), u.getBloqueadoHasta()).toMinutes() + 1;
                model.addAttribute("error",
                        "Cuenta bloqueada por demasiados intentos fallidos. Espera " + minutosRestantes + " minuto(s) o usa 'Olvidé mi contraseña'.");
                return "login";
            }
        }

        try {
            LoginRequest request = new LoginRequest();
            request.setUsuario(email);
            request.setPassword(password);

            LoginResponse response = loginService.login(request);

            Usuario usuario = loginService.obtenerUsuarioPorId(response.getIdUsuario());
            if (usuario == null) {
                model.addAttribute("error", "No se pudo recuperar el usuario desde la base de datos.");
                return "login";
            }

            // Login exitoso → resetear intentos fallidos
            if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
                usuarioRepository.save(usuario);
            }

            session.setAttribute("usuario", usuario);
            session.setAttribute("idUsuario", response.getIdUsuario());
            session.setAttribute("id_perfil", response.getIdPerfil());
            session.setAttribute("perfil", response.getPerfil());

            return switch (response.getPerfil()) {
                case "Aprendiz"       -> "redirect:/dashboard/aprendiz";
                case "Administrador"  -> "redirect:/dashboard/admin";
                case "Instructor"     -> {
                    if (usuario.getNoDocumento() != null && usuario.getNoDocumento().startsWith("PENDIENTE"))
                        yield "redirect:/completar-perfil";
                    yield "redirect:/dashboard/instructor";
                }
                case "Administrativo" -> "redirect:/dashboard/administrativo";
                case "Seguridad"      -> "redirect:/seguridad/dashboard";
                case "Invitado"       -> "redirect:/dashboard/invitado";
                default               -> { model.addAttribute("error", "Perfil no válido."); yield "login"; }
            };

        } catch (RuntimeException e) {
            // Login fallido → incrementar contador
            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                int intentos = u.getIntentosFallidos() + 1;
                u.setIntentosFallidos(intentos);
                if (intentos >= MAX_INTENTOS) {
                    u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MIN));
                    u.setIntentosFallidos(0);
                    usuarioRepository.save(u);
                    model.addAttribute("error",
                            "Demasiados intentos fallidos. Cuenta bloqueada " + BLOQUEO_MIN + " minutos.");
                } else {
                    usuarioRepository.save(u);
                    int restantes = MAX_INTENTOS - intentos;
                    model.addAttribute("error",
                            "Credenciales incorrectas. " + restantes + " intento(s) restante(s) antes del bloqueo.");
                }
            } else {
                model.addAttribute("error", "Usuario o contraseña incorrectos.");
            }
            return "login";
        }
    }

    @GetMapping("/salir")
    public String salir(HttpSession session) {
        session.invalidate();
        loginService.logout();
        return "redirect:/login";
    }
}
