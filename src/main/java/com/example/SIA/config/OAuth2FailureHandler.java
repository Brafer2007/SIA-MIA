package com.example.SIA.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Maneja los errores de autenticación OAuth2 (Google).
 * Redirige al login con un mensaje de error apropiado.
 */
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String errorCode = "google_error";

        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            String code = oauthEx.getError().getErrorCode();
            errorCode = switch (code) {
                case "user_not_registered" -> "user_not_registered";
                case "user_inactive"       -> "user_inactive";
                case "email_not_found"     -> "email_not_found";
                default                    -> "google_error";
            };
        }

        response.sendRedirect("/login?oauth2Error=" + errorCode);
    }
}
