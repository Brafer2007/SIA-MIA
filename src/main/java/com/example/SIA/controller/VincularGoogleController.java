package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Permite a un usuario ya autenticado (sesión activa) vincular o desvincular
 * su cuenta de Google.
 *
 * Flujo de vinculación:
 *  1. El usuario hace clic en "Vincular Google" desde su dashboard.
 *  2. Se redirige a /vincular-google/iniciar → guarda flag en sesión y
 *     redirige a /oauth2/authorization/google.
 *  3. Google autentica → OAuth2SuccessHandler detecta el flag y llama a
 *     POST /vincular-google/confirmar con el googleId obtenido.
 *
 * Para no duplicar el flujo OAuth2, el enlace de vinculación pasa por el mismo
 * endpoint de Google pero con un parámetro de sesión que indica "estoy vinculando,
 * no haciendo login".
 */
@Controller
@RequestMapping("/vincular-google")
public class VincularGoogleController {

    private final UsuarioRepository usuarioRepository;

    public VincularGoogleController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El usuario hace clic en "Vincular Google".
     * Guardamos en sesión que esto es una vinculación (no un login nuevo)
     * y redirigimos al flujo OAuth2 de Google.
     */
    @GetMapping("/iniciar")
    public String iniciar(HttpSession session) {
        // Solo usuarios con sesión activa
        if (session.getAttribute("idUsuario") == null) {
            return "redirect:/login";
        }
        // Marcar que este flujo OAuth2 es para vincular, no para hacer login
        session.setAttribute("oauth2_accion", "vincular");
        return "redirect:/oauth2/authorization/google";
    }

    /**
     * Llamado por OAuth2SuccessHandler cuando detecta que la acción es "vincular".
     * Recibe el googleId y lo asocia al usuario de la sesión actual.
     */
    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmar(
            @RequestParam String googleId,
            @RequestParam String emailGoogle,
            HttpSession session) {

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Sesión no encontrada. Inicia sesión primero."));
        }

        // Verificar que el googleId no esté ya usado por otro usuario
        Optional<Usuario> yaVinculado = usuarioRepository.findByGoogleId(googleId);
        if (yaVinculado.isPresent() && !yaVinculado.get().getIdUsuario().equals(idUsuario)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Esta cuenta de Google ya está vinculada a otro usuario del sistema."));
        }

        // Verificar que el correo Google no esté registrado en otro usuario
        Optional<Usuario> porCorreo = usuarioRepository.findByCorreoIgnoreCase(emailGoogle);
        if (porCorreo.isPresent() && !porCorreo.get().getIdUsuario().equals(idUsuario)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El correo de esta cuenta de Google pertenece a otro usuario registrado."));
        }

        // Vincular
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setGoogleId(googleId);
        usuarioRepository.save(usuario);

        // Actualizar sesión
        session.setAttribute("usuario", usuario);
        session.removeAttribute("oauth2_accion");

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta de Google vinculada correctamente."));
    }

    /**
     * Desvincula la cuenta de Google del usuario actual.
     * Solo aplica si el usuario tiene contraseña (puede seguir entrando por login normal).
     */
    @PostMapping("/desvincular")
    @ResponseBody
    public ResponseEntity<?> desvincular(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Sesión no encontrada."));
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // No permitir desvincular si no tiene contraseña (quedaría sin forma de entrar)
        if (usuario.getPassUsuario() == null || usuario.getPassUsuario().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error",
                    "No puedes desvincular Google porque tu cuenta no tiene contraseña configurada. " +
                    "Establece una contraseña primero."));
        }

        usuario.setGoogleId(null);
        usuarioRepository.save(usuario);
        session.setAttribute("usuario", usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta de Google desvinculada correctamente."));
    }

    /**
     * Devuelve el estado de vinculación del usuario actual (para la UI).
     */
    @GetMapping("/estado")
    @ResponseBody
    public ResponseEntity<?> estado(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean vinculado = usuario.getGoogleId() != null && !usuario.getGoogleId().isBlank();
        boolean tienePassword = usuario.getPassUsuario() != null && !usuario.getPassUsuario().isBlank();

        return ResponseEntity.ok(Map.of(
            "vinculado", vinculado,
            "tienePassword", tienePassword,
            "correo", usuario.getCorreo() != null ? usuario.getCorreo() : ""
        ));
    }
}
