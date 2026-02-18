package com.example.SIA.controller;

import com.example.SIA.dto.LoginRequest;
import com.example.SIA.dto.LoginResponse;
import com.example.SIA.entity.Usuario;
import com.example.SIA.service.LoginService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    // Mostrar vista login (GET /login)
    @GetMapping
    public String mostrarLogin() {
        return "login"; // Thymeleaf buscará templates/login.html
    }

    // Procesar formulario login (POST /login)
    @PostMapping
    public String acceder(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpSession session
    ) {
        try {
            LoginRequest request = new LoginRequest();
            request.setUsuario(email);
            request.setPassword(password);

            LoginResponse response = loginService.login(request);

            // ✅ Obtener el objeto Usuario completo
            Usuario usuario = loginService.obtenerUsuarioPorId(response.getIdUsuario());
            if (usuario == null) {
                model.addAttribute("error", "No se pudo recuperar el usuario desde la base de datos.");
                return "login";
            }

            // 👇 Guarda datos en sesión
            session.setAttribute("usuario", usuario);
            session.setAttribute("idUsuario", response.getIdUsuario());
            session.setAttribute("id_perfil", response.getIdPerfil());
            session.setAttribute("perfil", response.getPerfil());

            // ✅ Redirigir según perfil
            switch (response.getPerfil()) {
                case "Aprendiz":
                    return "redirect:/dashboard/aprendiz";
                case "Administrador":
                    return "redirect:/dashboard/admin";
                case "Instructor":
                    return "redirect:/dashboard/instructor";
                case "Administrativo":
                    return "redirect:/dashboard/administrativo";
                default:
                    model.addAttribute("error", "Perfil no válido");
                    return "login";
            }

        } catch (RuntimeException e) {
            e.printStackTrace(); // ✅ Muestra el error completo en consola
            model.addAttribute("error", "Error al iniciar sesión: " + e.getMessage());
            return "login";
        }
    }

    // Cerrar sesión
    @GetMapping("/salir")
    public String salir(HttpSession session) {
        session.invalidate(); // ✅ Invalida la sesión manualmente
        loginService.logout();
        return "redirect:/login";
    }
}