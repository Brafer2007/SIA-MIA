package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Notificacion;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.service.AprendizService;
import com.example.SIA.service.NotificacionService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DashboardController {

    private final UsuarioService usuarioService;
    private final AprendizService aprendizService;
    private final com.example.SIA.repository.ProgramacionRepository programacionRepository;
    private final NotificacionService notificacionService;

    // ✅ Inyección por constructor (más profesional)
    public DashboardController(
            UsuarioService usuarioService,
            AprendizService aprendizService,
            com.example.SIA.repository.ProgramacionRepository programacionRepository,
            NotificacionService notificacionService
    ) {
        this.usuarioService = usuarioService;
        this.aprendizService = aprendizService;
        this.programacionRepository = programacionRepository;
        this.notificacionService = notificacionService;
    }

    // ============================================================
    // REDIRECCIÓN SEGÚN PERFIL
    // ============================================================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        Integer perfil = (Integer) session.getAttribute("id_perfil");

        if (perfil == null) {
            return "redirect:/login";
        }

        return switch (perfil) {
            case 1 -> "redirect:/dashboard/aprendiz";
            case 2 -> "redirect:/dashboard/admin";
            case 3 -> "redirect:/dashboard/instructor";
            default -> "redirect:/login?error=perfil_no_valido";
        };
    }

    // ============================================================
    // PANEL APRENDIZ
    // ============================================================
    @GetMapping("/dashboard/aprendiz")
    public String dashboardAprendiz(HttpSession session, Model model) {

        if (!perfilValido(session, 1)) {
            return "redirect:/login?error=acceso_denegado";
        }

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Usuario usuario = usuarioService.findById(idUsuario);

        model.addAttribute("idUsuario", idUsuario);

        List<Aprendiz> aprendices = aprendizService.findByUsuarioId(idUsuario);
        Aprendiz aprendiz = aprendices.isEmpty() ? new Aprendiz() : aprendices.get(0);

        if (aprendiz.getFichaFormacion() == null) aprendiz.setFichaFormacion("");
        if (aprendiz.getProgramaFormacion() == null) aprendiz.setProgramaFormacion("");

        // Usar el campo perfilCompleto de la BD, o calcular basado en los datos si no existe
        boolean aprendizCompleto = aprendiz.getPerfilCompleto() != null && aprendiz.getPerfilCompleto() == 1;
        if (!aprendizCompleto && !aprendiz.getFichaFormacion().isEmpty() && !aprendiz.getProgramaFormacion().isEmpty()) {
            aprendizCompleto = true;
        }

        // ✅ Equipos del aprendiz
        List<com.example.SIA.entity.Equipo> equipos;
        try {
            equipos = ((com.example.SIA.service.EquipoService)
                    org.springframework.beans.factory.BeanFactoryUtils
                            .beanOfTypeIncludingAncestors(
                                    org.springframework.web.context.support.WebApplicationContextUtils
                                            .getWebApplicationContext(session.getServletContext()),
                                    com.example.SIA.service.EquipoService.class))
                    .listarPorUsuario(idUsuario);
        } catch (Exception e) {
            equipos = new java.util.ArrayList<>();
        }

        model.addAttribute("equipos", equipos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("aprendiz", aprendiz);
        model.addAttribute("aprendiz_completo", aprendizCompleto);

        // ✅ Horario
        if (aprendizCompleto) {
            List<com.example.SIA.entity.Programacion> horario =
                    programacionRepository.findByNombreFicha(aprendiz.getFichaFormacion());
            model.addAttribute("horario", horario);
        } else {
            model.addAttribute("horario", new java.util.ArrayList<>());
        }

        return "dashboardAprendiz";
    }

    // ============================================================
    // PANEL ADMINISTRADOR
    // ============================================================
    @GetMapping("/dashboard/admin")
    public String dashboardAdministrador(HttpSession session, Model model) {

        if (!perfilValido(session, 2)) {
            return "redirect:/login?error=acceso_denegado";
        }

        model.addAttribute("usuarios", usuarioService.findAll());
        return "dashboardAdministrador";
    }

    // ✅ Obtener notificaciones NO leídas
    @GetMapping("/dashboard/admin/notificaciones")
    @ResponseBody
    public List<Notificacion> obtenerNotificaciones() {
        return notificacionService.obtenerNoLeidas();
    }
    

    // ✅ Marcar notificaciones como leídas
    @PostMapping("/dashboard/admin/notificaciones/leidas")
    @ResponseBody
    public void marcarLeidas() {
        notificacionService.marcarComoLeidas();
    }

    // ============================================================
    // PANEL INSTRUCTOR
    // ============================================================
    @GetMapping("/dashboard/instructor")
    public String dashboardInstructor(HttpSession session, Model model) {

        if (!perfilValido(session, 3)) {
            return "redirect:/login?error=acceso_denegado";
        }

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        model.addAttribute("idUsuario", idUsuario);

        Usuario usuario = usuarioService.findById(idUsuario);

        model.addAttribute("usuario", usuario.getNombres() + " " + usuario.getApellidos());

        // ✅ ID del instructor
        if (usuario.getInstructor() != null) {
            model.addAttribute("idInstructor", usuario.getInstructor().getId());
        } else {
            model.addAttribute("idInstructor", 0);
        }

        // ✅ Equipos del instructor
        List<com.example.SIA.entity.Equipo> equipos;
        try {
            equipos = ((com.example.SIA.service.EquipoService)
                    org.springframework.beans.factory.BeanFactoryUtils
                            .beanOfTypeIncludingAncestors(
                                    org.springframework.web.context.support.WebApplicationContextUtils
                                            .getWebApplicationContext(session.getServletContext()),
                                    com.example.SIA.service.EquipoService.class))
                    .listarPorUsuario(idUsuario);
        } catch (Exception e) {
            equipos = new java.util.ArrayList<>();
        }

        model.addAttribute("equipos", equipos);

        return "dashboardInstructor";
    }

    // ============================================================
    // VALIDACIÓN DE PERFIL
    // ============================================================
    private boolean perfilValido(HttpSession session, int perfilEsperado) {
        Integer perfil = (Integer) session.getAttribute("id_perfil");
        return perfil != null && perfil == perfilEsperado;
    }
}