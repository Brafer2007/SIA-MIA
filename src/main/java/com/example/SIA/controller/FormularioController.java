package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.service.AprendizService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/aprendiz")
public class FormularioController {
    private final UsuarioService usuarioService;
    private final AprendizService aprendizService;

    public FormularioController(UsuarioService usuarioService, AprendizService aprendizService) {
        this.usuarioService = usuarioService;
        this.aprendizService = aprendizService;
    }

    @GetMapping("/formulario")
    public String mostrarFormulario(HttpSession session, Model model) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Usuario usuario = usuarioService.findById(idUsuario);
        java.util.List<Aprendiz> aprendices = aprendizService.findByUsuarioId(idUsuario);
        Aprendiz aprendiz = aprendices.isEmpty() ? new Aprendiz() : aprendices.get(0);

        boolean perfilCompleto = aprendiz != null && aprendiz.getPerfilCompleto() != null && aprendiz.getPerfilCompleto() == 1;

        List<String> programas = Arrays.asList(
            "Análisis y Desarrollo de Software",
            "Gestión de Redes de Datos",
            "Gestión Administrativa",
            "Contabilidad y Finanzas",
            "Diseño e Integración de Multimedia",
            "Mecatrónica",
            "Seguridad y Salud en el Trabajo"
        );

        Integer idPerfil = (Integer) session.getAttribute("id_perfil");
        String dashboardUrl = (idPerfil != null && idPerfil == 1)
            ? "/dashboard/aprendiz?section=perfil"
            : "/dashboard/invitado?section=perfil";

        model.addAttribute("usuario", usuario);
        model.addAttribute("aprendiz", aprendiz);
        model.addAttribute("programas", programas);
        model.addAttribute("perfilCompleto", perfilCompleto);
        model.addAttribute("dashboardUrl", dashboardUrl);
        return "formulario";
    }

    @PostMapping("/formulario/guardar")
    public String guardarFormulario(
            HttpSession session,
            @ModelAttribute("usuario") Usuario usuarioForm,
            @ModelAttribute("aprendiz") Aprendiz aprendizForm,
            RedirectAttributes redirectAttrs) {

        Integer idUsuario = (Integer) session.getAttribute("idUsuario");

        usuarioForm.setIdUsuario(idUsuario);
        Usuario original = usuarioService.findById(idUsuario);

        if (usuarioForm.getPassUsuario() == null || usuarioForm.getPassUsuario().isBlank()) {
            if (original != null) {
                usuarioForm.setPassUsuario(original.getPassUsuario());
            }
        }

        Usuario usuarioEntity = usuarioService.findById(idUsuario);
        aprendizForm.setUsuario(usuarioEntity);

        // Si ficha/programa vienen vacíos (perfil ya completo), conservar los valores originales
        List<Aprendiz> aprendicesOriginales = aprendizService.findByUsuarioId(idUsuario);
        Aprendiz aprendizOriginal = aprendicesOriginales.isEmpty() ? null : aprendicesOriginales.get(0);
        if (aprendizOriginal != null) {
            if (aprendizForm.getFichaFormacion() == null || aprendizForm.getFichaFormacion().isBlank())
                aprendizForm.setFichaFormacion(aprendizOriginal.getFichaFormacion());
            if (aprendizForm.getProgramaFormacion() == null || aprendizForm.getProgramaFormacion().isBlank())
                aprendizForm.setProgramaFormacion(aprendizOriginal.getProgramaFormacion());
            if (aprendizForm.getIdAprendiz() == null)
                aprendizForm.setIdAprendiz(aprendizOriginal.getIdAprendiz());
        }

        // Marcar perfil como completo cuando tenga los datos requeridos
        if (aprendizForm.getFichaFormacion() != null && !aprendizForm.getFichaFormacion().isBlank() &&
            aprendizForm.getProgramaFormacion() != null && !aprendizForm.getProgramaFormacion().isBlank() &&
            usuarioForm.getNombres() != null && !usuarioForm.getNombres().isBlank() &&
            usuarioForm.getApellidos() != null && !usuarioForm.getApellidos().isBlank()) {
            aprendizForm.setPerfilCompleto(1);
        } else {
            aprendizForm.setPerfilCompleto(0);
        }

        usuarioService.actualizar(usuarioForm);
        aprendizService.actualizarAprendiz(aprendizForm);

        // Actualizar sesión con datos frescos
        session.setAttribute("usuario", usuarioService.findById(idUsuario));

        redirectAttrs.addFlashAttribute("success", "Datos actualizados correctamente.");

        // Redirigir al dashboard correcto según perfil
        Integer idPerfil = (Integer) session.getAttribute("id_perfil");
        String destino = (idPerfil != null && idPerfil == 1)
            ? "/dashboard/aprendiz?section=perfil"
            : "/dashboard/invitado?section=perfil";
        return "redirect:" + destino;
    }
}
