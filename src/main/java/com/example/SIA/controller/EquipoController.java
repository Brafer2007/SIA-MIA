package com.example.SIA.controller;

import com.example.SIA.dto.EquipoDTO;
import com.example.SIA.entity.Equipo;
import com.example.SIA.service.EquipoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    // Mostrar lista de equipos del usuario
    @GetMapping
    public String mostrarEquipos(HttpSession session, Model model) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            return "redirect:/login";
        }

        List<Equipo> equipos = equipoService.listarPorUsuario(idUsuario);
        model.addAttribute("equipos", equipos);

        // Determinar URL del dashboard según perfil
        String dashboardUrl = "Aprendiz".equals(perfil) ? "/dashboard/aprendiz" : "/dashboard/instructor";
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "equipos"; // vista común para ambos
    }

    // Mostrar formulario de registro de equipo
    @GetMapping("/registrar")
    public String mostrarRegistrarEquipo(HttpSession session, Model model) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("equipoDTO", new EquipoDTO());

        // URL para volver al dashboard
        String dashboardUrl = "Aprendiz".equals(perfil) ? "/dashboard/aprendiz" : "/dashboard/instructor";
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "equipos";
    }

    // Fallback para asegurar dashboardUrl en cualquier retorno a 'equipos'
    @ModelAttribute
    public void addDashboardUrlToModel(HttpSession session, Model model) {
        if (!model.containsAttribute("dashboardUrl")) {
            String perfil = (String) session.getAttribute("perfil");
            String dashboardUrl;
            if ("Aprendiz".equals(perfil)) {
                dashboardUrl = "/dashboard/aprendiz";
            } else if ("Instructor".equals(perfil)) {
                dashboardUrl = "/dashboard/instructor";
            } else {
                dashboardUrl = "/login";
            }
            model.addAttribute("dashboardUrl", dashboardUrl);
        }
    }

    // Registrar equipo
    @PostMapping("/registrar")
    public String registrarEquipo(@ModelAttribute EquipoDTO equipoDTO,
                                  HttpSession session,
                                  RedirectAttributes redirectAttrs) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            redirectAttrs.addFlashAttribute("error", "Debes iniciar sesión para registrar un equipo");
            return "redirect:/login";
        }

        equipoDTO.setIdUsuario(idUsuario);
        try {
            equipoService.registrarEquipo(equipoDTO);
            redirectAttrs.addFlashAttribute("mensaje", "✅ Equipo registrado correctamente");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", "❌ Error: " + e.getMessage());
        }

        return "Aprendiz".equals(perfil)
                ? "redirect:/dashboard/aprendiz#equipos"
                : "redirect:/dashboard/instructor#equipos";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarEditarEquipo(@PathVariable("id") Integer idEquipo,
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttrs) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            return "redirect:/login";
        }

        Equipo equipo = equipoService.buscarPorIdYUsuario(idEquipo, idUsuario);
        if (equipo == null) {
            redirectAttrs.addFlashAttribute("error", "No autorizado o equipo no encontrado");
            return "Aprendiz".equals(perfil)
                    ? "redirect:/dashboard/aprendiz#equipos"
                    : "redirect:/dashboard/instructor#equipos";
        }

        model.addAttribute("equipo", equipo);

        // URL para volver al dashboard
        String dashboardUrl = "Aprendiz".equals(perfil) ? "/dashboard/aprendiz" : "/dashboard/instructor";
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "editarEquipo"; // vista de edición
    }

    // Actualizar equipo
    @PostMapping("/editar/{id}")
    public String actualizarEquipo(@PathVariable("id") Integer idEquipo,
                                   @ModelAttribute EquipoDTO equipoDTO,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            return "redirect:/login";
        }

        equipoDTO.setIdUsuario(idUsuario);
        equipoDTO.setIdEquipo(idEquipo);

        try {
            equipoService.actualizarEquipo(equipoDTO);
            redirectAttrs.addFlashAttribute("mensaje", "✅ Equipo actualizado correctamente");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/equipos/editar/" + idEquipo;
        }

        return "Aprendiz".equals(perfil)
                ? "redirect:/dashboard/aprendiz#equipos"
                : "redirect:/dashboard/instructor#equipos";
    }

    // Desactivar equipo
    @PostMapping("/desactivar/{id}")
    public String desactivarEquipo(@PathVariable("id") Integer idEquipo,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        String perfil = (String) session.getAttribute("perfil");

        if (idUsuario == null) {
            return "redirect:/login";
        }

        equipoService.desactivarEquipo(idEquipo, idUsuario);
        redirectAttrs.addFlashAttribute("mensaje", "El equipo se ha eliminado correctamente.");

        return "Aprendiz".equals(perfil)
                ? "redirect:/dashboard/aprendiz#equipos"
                : "redirect:/dashboard/instructor#equipos";
    }
}
