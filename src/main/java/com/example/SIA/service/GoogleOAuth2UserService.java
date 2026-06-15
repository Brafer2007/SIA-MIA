package com.example.SIA.service;

import com.example.SIA.entity.Perfil;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.PerfilRepository;
import com.example.SIA.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio OAuth2 de Google.
 *
 * Escenarios:
 *  A) googleId ya vinculado a un usuario → login directo.
 *  B) Correo ya existe pero sin googleId → vincula automáticamente y hace login.
 *  C) Correo no existe → crea usuario nuevo con perfil Invitado (datos pendientes).
 */
@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2UserService.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public GoogleOAuth2UserService(UsuarioRepository usuarioRepository,
                                   PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email     = oAuth2User.getAttribute("email");
        String googleId  = oAuth2User.getAttribute("sub");
        String nombres   = oAuth2User.getAttribute("given_name");
        String apellidos = oAuth2User.getAttribute("family_name");
        String nombre    = oAuth2User.getAttribute("name");

        log.info("[OAuth2] Intento de login — email: {}, googleId: {}", email, googleId);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "No se pudo obtener el correo desde la cuenta de Google."
            );
        }

        try {
            // A) ¿Ya hay un usuario vinculado a este googleId?
            Optional<Usuario> porGoogleId = usuarioRepository.findByGoogleId(googleId);
            if (porGoogleId.isPresent()) {
                log.info("[OAuth2] Usuario encontrado por googleId: {}", porGoogleId.get().getIdUsuario());
                validarActivo(porGoogleId.get());
                return oAuth2User;
            }

            // B) ¿Existe el correo en el sistema?
            Optional<Usuario> porCorreo = usuarioRepository.findByCorreoIgnoreCase(email);
            if (porCorreo.isPresent()) {
                Usuario u = porCorreo.get();
                log.info("[OAuth2] Usuario encontrado por correo: {}. Vinculando googleId.", u.getIdUsuario());
                validarActivo(u);
                u.setGoogleId(googleId);
                usuarioRepository.save(u);
                return oAuth2User;
            }

            // C) Usuario nuevo — crear con perfil Invitado
            log.info("[OAuth2] Usuario nuevo. Creando cuenta para: {}", email);
            Usuario nuevo = new Usuario();
            nuevo.setGoogleId(googleId);
            nuevo.setCorreo(email);
            nuevo.setEstado(1);

            if (nombres != null && !nombres.isBlank()) {
                nuevo.setNombres(nombres);
            } else if (nombre != null && !nombre.isBlank()) {
                nuevo.setNombres(nombre);
            } else {
                nuevo.setNombres("Usuario");
            }

            nuevo.setApellidos((apellidos != null && !apellidos.isBlank()) ? apellidos : "Google");

            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9.]", ".");
            nuevo.setNombreUsuario(generarUsernameUnico(baseUsername));

            nuevo.setNoDocumento("GOOGLE_PENDIENTE_" + googleId.substring(0, 8));
            nuevo.setPassUsuario(null);

            Perfil invitado = perfilRepository.findByNombrePerfil("Invitado").orElseGet(() -> {
                Perfil p = new Perfil();
                p.setNombrePerfil("Invitado");
                return perfilRepository.save(p);
            });
            nuevo.setPerfil(invitado);

            Usuario guardado = usuarioRepository.save(nuevo);
            log.info("[OAuth2] Usuario nuevo creado con id: {}", guardado.getIdUsuario());

        } catch (OAuth2AuthenticationException e) {
            throw e; // re-lanzar excepciones propias (user_inactive, etc.)
        } catch (Exception e) {
            log.error("[OAuth2] Error al procesar usuario de Google: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                new OAuth2Error("server_error"),
                "Error interno al procesar la autenticación con Google: " + e.getMessage()
            );
        }

        return oAuth2User;
    }

    private void validarActivo(Usuario u) throws OAuth2AuthenticationException {
        if (u.getEstado() == null || u.getEstado() != 1) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("user_inactive"),
                "Esta cuenta está inactiva. Contacta al administrador."
            );
        }
    }

    private String generarUsernameUnico(String base) {
        String candidate = base;
        int suffix = 1;
        while (usuarioRepository.existsByNombreUsuario(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
