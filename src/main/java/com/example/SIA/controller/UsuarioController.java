package com.example.SIA.controller;

import com.example.SIA.dto.*;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Usuario;
import com.example.SIA.service.AprendizService;
import com.example.SIA.service.PerfilService;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.util.ExcelExporter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;
    private final AprendizService aprendizService;

    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService, AprendizService aprendizService) {
        this.usuarioService = usuarioService;
        this.perfilService = perfilService;
        this.aprendizService = aprendizService;
    }

    // ============================
    //     VISTAS THYMELEAF
    // ============================

    // Mostrar formulario de nuevo usuario
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("usuario", new UsuarioRequest());
        return "nuevo"; // nuevo.html
    }

    // Procesar formulario de nuevo usuario
    @PostMapping("/nuevo")
    public String guardarUsuario(@ModelAttribute UsuarioRequest request) {
        usuarioService.crearUsuario(request);
        return "redirect:/dashboard/admin";
    }

    // Listado de usuarios en vista
    @GetMapping("/lista")
    public String listarUsuarios(Model model) {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuario_listado"; // usuario_listado.html
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Integer id, Model model) {
        Usuario usuario = usuarioService.findById(id);
        if (usuario == null) return "redirect:/dashboard/admin";

        UsuarioUpdateRequest updateRequest = new UsuarioUpdateRequest();
        updateRequest.setIdUsuario(usuario.getIdUsuario());
        updateRequest.setNombreUsuario(usuario.getNombreUsuario());
        updateRequest.setNombres(usuario.getNombres());
        updateRequest.setApellidos(usuario.getApellidos());
        updateRequest.setCorreo(usuario.getCorreo());
        updateRequest.setNoDocumento(usuario.getNoDocumento());
        if (usuario.getPerfil() != null) {
            updateRequest.setIdPerfil(usuario.getPerfil().getIdPerfil());
        }

        // Cargar datos de aprendiz si existen
        List<Aprendiz> aprendices = aprendizService.findByUsuarioId(id);
        if (!aprendices.isEmpty()) {
            Aprendiz aprendiz = aprendices.get(0);
            updateRequest.setFichaFormacion(aprendiz.getFichaFormacion());
            updateRequest.setProgramaFormacion(aprendiz.getProgramaFormacion());
        }

        model.addAttribute("perfiles", perfilService.findAll());
        model.addAttribute("usuario", updateRequest);
        model.addAttribute("programas", Arrays.asList(
            "Análisis y Desarrollo de Software",
            "Gestión de Redes de Datos",
            "Gestión Administrativa",
            "Contabilidad y Finanzas",
            "Diseño e Integración de Multimedia",
            "Mecatrónica",
            "Seguridad y Salud en el Trabajo"
        ));
        return "editar";
    }

    // Procesar formulario de edición
    @PostMapping("/editar")
    public String actualizarUsuario(@ModelAttribute UsuarioUpdateRequest request) {
        usuarioService.actualizarUsuario(request);

        // Actualizar ficha y programa solo si el usuario es Aprendiz y hay datos reales
        boolean tieneFicha = request.getFichaFormacion() != null && !request.getFichaFormacion().isBlank();
        boolean tienePrograma = request.getProgramaFormacion() != null && !request.getProgramaFormacion().isBlank();
        if (tieneFicha || tienePrograma) {
            List<Aprendiz> aprendices = aprendizService.findByUsuarioId(request.getIdUsuario());
            // Solo actualizar si ya existe el registro de aprendiz, o si hay ficha Y programa
            if (!aprendices.isEmpty() || (tieneFicha && tienePrograma)) {
                Aprendiz aprendiz = aprendices.isEmpty() ? new Aprendiz() : aprendices.get(0);
                Usuario usuarioEntity = usuarioService.findById(request.getIdUsuario());
                aprendiz.setUsuario(usuarioEntity);
                if (tieneFicha) aprendiz.setFichaFormacion(request.getFichaFormacion());
                if (tienePrograma) aprendiz.setProgramaFormacion(request.getProgramaFormacion());
                if (aprendiz.getFichaFormacion() != null && !aprendiz.getFichaFormacion().isBlank()
                        && aprendiz.getProgramaFormacion() != null && !aprendiz.getProgramaFormacion().isBlank())
                    aprendiz.setPerfilCompleto(1);
                aprendizService.actualizarAprendiz(aprendiz);
            }
        }

        return "redirect:/dashboard/admin";
    }

    // ============================
    //     API REST (JSON)
    // ============================

    @ResponseBody
    @GetMapping
    public List<UsuarioResponse> index() {
        return usuarioService.listarUsuarios();
    }

    @ResponseBody
    @PostMapping("/agregar")
    public UsuarioResponse agregar(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crearUsuario(request);
    }

    @PostMapping("/activar/{id}")
@ResponseBody
public Map<String, Object> activarUsuario(@PathVariable("id") Integer id) {
    Map<String, Object> response = new HashMap<>();
    try {
        usuarioService.activar(id);
        response.put("success", true);
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", e.getMessage());
    }
    return response;
}

@PostMapping("/desactivar/{id}")
@ResponseBody
public Map<String, Object> desactivarUsuario(@PathVariable("id") Integer id) {
    Map<String, Object> response = new HashMap<>();
    try {
        usuarioService.desactivar(id);
        response.put("success", true);
    } catch (Exception e) {
        response.put("success", false);
        response.put("message", e.getMessage());
    }
    return response;
}


    @ResponseBody
    @GetMapping("/exportarExcel")
    public ResponseEntity<byte[]> exportarExcel() {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();
        byte[] excel = ExcelExporter.exportarUsuarios(usuarios);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=usuarios.xlsx")
                .body(excel);
    }
}
