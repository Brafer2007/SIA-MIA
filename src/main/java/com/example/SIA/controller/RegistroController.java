package com.example.SIA.controller;

import com.example.SIA.dto.UsuarioRegistroRequest;
import com.example.SIA.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Mostrar el formulario de registro
    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("usuario", new UsuarioRegistroRequest());
        return "registro"; // Thymeleaf buscará registro.html en templates
    }

    // Procesar el registro
    @PostMapping("/guardar")
    public String guardarUsuario(
            @Valid @ModelAttribute("usuario") UsuarioRegistroRequest usuario,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "registro"; // volver al formulario con errores
        }

        try {
            usuarioService.registrarUsuario(usuario);
            model.addAttribute("mensaje", "Usuario registrado correctamente");
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error: " + e.getMessage());
            return "registro";
        }

        return "login"; // mostrar el formulario con mensaje de éxito
    }
}
