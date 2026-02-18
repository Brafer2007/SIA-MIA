package com.example.SIA.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusCode != null) {
            int status = Integer.parseInt(statusCode.toString());

            // 👇 Log para verificar que el controlador se ejecuta
            System.out.println("Entrando al controlador de error con código: " + status);

            switch (status) {
                case 404:
                    return "error/404";
                case 500:
                    return "error/500";
                case 403:
                    return "error/403";
                case 502:
                    return "error/502";
                default:
                    return "error/generic";
            }
        }

        System.out.println("No se recibió código de error, mostrando vista genérica.");
        return "error/generic";
    }
}