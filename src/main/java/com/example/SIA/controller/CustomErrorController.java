package com.example.SIA.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpSession session, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // Determinar URL de retorno según perfil de sesión
        String backUrl = resolverBackUrl(session, request);
        model.addAttribute("backUrl", backUrl);

        // Mensaje de error original si existe
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (errorMessage != null && !errorMessage.toString().isBlank()) {
            model.addAttribute("errorMessage", errorMessage.toString());
        }

        if (statusCode != null) {
            int status = Integer.parseInt(statusCode.toString());
            return switch (status) {
                case 404 -> "error/404";
                case 403 -> "error/403";
                case 500 -> "error/500";
                case 502 -> "error/502";
                default  -> "error/generic";
            };
        }

        return "error/generic";
    }

    private String resolverBackUrl(HttpSession session, HttpServletRequest request) {
        // Si hay sesión activa, redirigir al dashboard del perfil
        if (session != null) {
            Integer perfil = (Integer) session.getAttribute("id_perfil");
            if (perfil != null) {
                return switch (perfil) {
                    case 1 -> "/dashboard/aprendiz";
                    case 2 -> "/dashboard/admin";
                    case 3 -> "/dashboard/instructor";
                    case 5 -> "/seguridad/dashboard";
                    default -> "/dashboard";
                };
            }
        }
        // Sin sesión: intentar usar el Referer, si no, ir al inicio
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/error")) {
            return referer;
        }
        return "/";
    }
}
