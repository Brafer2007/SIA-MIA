package com.example.SIA.config;

import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Maneja el éxito de autenticación OAuth2 (Google).
 *
 * Tres escenarios:
 *  1. La sesión tiene oauth2_accion=vincular  → confirmar vinculación y volver al dashboard.
 *  2. Usuario con datos completos             → carga sesión y redirige por perfil.
 *  3. Usuario nuevo (GOOGLE_PENDIENTE)        → redirige a /completar-perfil-google.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    public OAuth2SuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email    = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        HttpSession session = request.getSession(false);

        // ── Escenario 1: el usuario ya está en sesión y quiere vincular Google ──
        if (session != null
                && "vincular".equals(session.getAttribute("oauth2_accion"))
                && session.getAttribute("idUsuario") != null) {

            session.removeAttribute("oauth2_accion");

            // Verificar que el googleId no esté ya en uso por OTRO usuario
            Optional<Usuario> yaVinculado = usuarioRepository.findByGoogleId(googleId);
            Integer idActual = (Integer) session.getAttribute("idUsuario");

            if (yaVinculado.isPresent() && !yaVinculado.get().getIdUsuario().equals(idActual)) {
                String msg = URLEncoder.encode(
                    "Esta cuenta de Google ya está vinculada a otro usuario.",
                    StandardCharsets.UTF_8);
                response.sendRedirect("/dashboard?vincularError=" + msg);
                return;
            }

            // Verificar que el correo Google no esté en otro usuario
            Optional<Usuario> porCorreo = usuarioRepository.findByCorreoIgnoreCase(email);
            if (porCorreo.isPresent() && !porCorreo.get().getIdUsuario().equals(idActual)) {
                String msg = URLEncoder.encode(
                    "El correo de esta cuenta de Google ya pertenece a otro usuario registrado.",
                    StandardCharsets.UTF_8);
                response.sendRedirect("/dashboard?vincularError=" + msg);
                return;
            }

            // Vincular
            Usuario usuario = usuarioRepository.findById(idActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            usuario.setGoogleId(googleId);
            usuarioRepository.save(usuario);
            session.setAttribute("usuario", usuario);

            // Redirigir al dashboard correspondiente con mensaje de éxito
            String dashboard = getDashboardUrl(usuario.getPerfil().getNombrePerfil());
            response.sendRedirect(dashboard + "?vincularOk=true");
            return;
        }

        // ── Escenarios 2 y 3: login con Google ──────────────────────────────────
        Optional<Usuario> usuarioOpt = usuarioRepository.findByGoogleId(googleId);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(email);
        }

        if (usuarioOpt.isEmpty()) {
            response.sendRedirect("/login?oauth2Error=registro_fallido");
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Cargar datos de sesión SIA sobre la sesión existente de Spring Security
        // NO invalidar la sesión — Spring Security ya la gestiona correctamente
        HttpSession siaSession = request.getSession(true);
        siaSession.setAttribute("usuario", usuario);
        siaSession.setAttribute("idUsuario", usuario.getIdUsuario());
        siaSession.setAttribute("id_perfil", usuario.getPerfil().getIdPerfil());
        siaSession.setAttribute("perfil", usuario.getPerfil().getNombrePerfil());

        // ¿Datos incompletos? → formulario de completar
        if (usuario.getNoDocumento() != null
                && usuario.getNoDocumento().startsWith("GOOGLE_PENDIENTE")) {
            response.sendRedirect("/completar-perfil-google");
            return;
        }

        // Redirigir por perfil
        response.sendRedirect(getFullRedirect(usuario));
    }

    // ─── helpers ────────────────────────────────────────────────

    private String getFullRedirect(Usuario usuario) {
        String perfil = usuario.getPerfil().getNombrePerfil();
        if ("Instructor".equals(perfil)
                && usuario.getNoDocumento() != null
                && usuario.getNoDocumento().startsWith("PENDIENTE")) {
            return "/completar-perfil";
        }
        return getDashboardUrl(perfil);
    }

    private String getDashboardUrl(String perfil) {
        return switch (perfil) {
            case "Aprendiz"       -> "/dashboard/aprendiz";
            case "Administrador"  -> "/dashboard/admin";
            case "Instructor"     -> "/dashboard/instructor";
            case "Administrativo" -> "/dashboard/administrativo";
            case "Seguridad"      -> "/seguridad/dashboard";
            default               -> "/dashboard/invitado";
        };
    }
}
