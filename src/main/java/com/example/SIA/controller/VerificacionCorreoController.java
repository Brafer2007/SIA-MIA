package com.example.SIA.controller;

import com.example.SIA.entity.TokenVerificacion;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.TokenVerificacionRepository;
import com.example.SIA.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Verifica el correo electrónico del usuario tras el registro.
 * El enlace llega por correo y activa la cuenta (estado 0 → 1).
 */
@Controller
@RequestMapping("/verificar-correo")
public class VerificacionCorreoController {

    private final TokenVerificacionRepository tokenRepo;
    private final UsuarioRepository usuarioRepo;

    public VerificacionCorreoController(TokenVerificacionRepository tokenRepo,
                                        UsuarioRepository usuarioRepo) {
        this.tokenRepo   = tokenRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping
    public String verificar(@RequestParam String token, RedirectAttributes ra) {
        Optional<TokenVerificacion> opt = tokenRepo.findByToken(token);

        if (opt.isEmpty() || !opt.get().isValido()) {
            ra.addFlashAttribute("error",
                    "El enlace de verificación ha expirado o ya fue usado. Regístrate de nuevo.");
            return "redirect:/registro";
        }

        TokenVerificacion tv = opt.get();
        Optional<Usuario> usuOpt = usuarioRepo.findByCorreoIgnoreCase(tv.getCorreo());

        if (usuOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No se encontró el usuario. Intenta registrarte de nuevo.");
            return "redirect:/registro";
        }

        Usuario u = usuOpt.get();
        u.setEstado(1);  // activar cuenta
        usuarioRepo.save(u);

        tv.setUsado(true);
        tokenRepo.save(tv);

        ra.addFlashAttribute("exito", "¡Correo verificado! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}
