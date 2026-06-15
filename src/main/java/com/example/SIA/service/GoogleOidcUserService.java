package com.example.SIA.service;

import com.example.SIA.entity.Perfil;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.PerfilRepository;
import com.example.SIA.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio OIDC para Google (scope openid).
 * Google siempre usa el flujo OpenID Connect cuando el scope incluye "openid",
 * por lo que este es el servicio que realmente se invoca.
 *
 * Escenarios:
 *  A) googleId ya vinculado → login directo.
 *  B) Correo existe sin googleId → vincula automáticamente.
 *  C) Correo no existe → crea usuario Invitado con datos pendientes.
 */
@Service
public class GoogleOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOidcUserService.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    public GoogleOidcUserService(UsuarioRepository usuarioRepository,
                                 PerfilRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegar al OidcUserService base para obtener el OidcUser con todos los claims
        OidcUser oidcUser = super.loadUser(userRequest);

        String email     = oidcUser.getEmail();
        String googleId  = oidcUser.getSubject();   // claim "sub" = ID único de Google
        String nombres   = oidcUser.getGivenName();
        String apellidos = oidcUser.getFamilyName();
        String nombre    = oidcUser.getFullName();

        log.info("[OIDC] Intento de login — email: {}, googleId: {}", email, googleId);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "No se pudo obtener el correo desde Google."
            );
        }

        try {
            // A) ¿Ya vinculado por googleId?
            Optional<Usuario> porGoogleId = usuarioRepository.findByGoogleId(googleId);
            if (porGoogleId.isPresent()) {
                log.info("[OIDC] Usuario encontrado por googleId: {}", porGoogleId.get().getIdUsuario());
                validarActivo(porGoogleId.get());
                return oidcUser;
            }

            // B) ¿Existe el correo?
            Optional<Usuario> porCorreo = usuarioRepository.findByCorreoIgnoreCase(email);
            if (porCorreo.isPresent()) {
                Usuario u = porCorreo.get();
                log.info("[OIDC] Usuario encontrado por correo id={}. Vinculando googleId.", u.getIdUsuario());
                validarActivo(u);
                u.setGoogleId(googleId);
                usuarioRepository.save(u);
                return oidcUser;
            }

            // C) Usuario nuevo
            log.info("[OIDC] Usuario nuevo. Creando cuenta para: {}", email);
            Usuario nuevo = new Usuario();
            nuevo.setGoogleId(googleId);
            nuevo.setCorreo(email);
            nuevo.setEstado(1);
            nuevo.setNombres(nombres != null && !nombres.isBlank() ? nombres
                           : nombre  != null && !nombre.isBlank()  ? nombre
                           : "Usuario");
            nuevo.setApellidos(apellidos != null && !apellidos.isBlank() ? apellidos : "Google");

            String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9.]", ".");
            nuevo.setNombreUsuario(generarUsernameUnico(base));
            nuevo.setNoDocumento("GOOGLE_PENDIENTE_" + googleId.substring(0, 8));
            nuevo.setPassUsuario(null);

            Perfil invitado = perfilRepository.findByNombrePerfil("Invitado").orElseGet(() -> {
                Perfil p = new Perfil();
                p.setNombrePerfil("Invitado");
                return perfilRepository.save(p);
            });
            nuevo.setPerfil(invitado);

            Usuario guardado = usuarioRepository.save(nuevo);
            log.info("[OIDC] Usuario nuevo creado con id: {}", guardado.getIdUsuario());

        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OIDC] Error al procesar usuario de Google: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                new OAuth2Error("server_error"),
                "Error interno: " + e.getMessage()
            );
        }

        return oidcUser;
    }

    private void validarActivo(Usuario u) {
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
