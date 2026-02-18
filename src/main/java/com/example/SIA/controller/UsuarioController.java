package com.example.SIA.controller;

import com.example.SIA.dto.*;
import com.example.SIA.entity.Usuario;
import com.example.SIA.service.PerfilService;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.util.ExcelExporter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;

    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService) {
        this.usuarioService = usuarioService;
        this.perfilService = perfilService;
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

        if (usuario == null) {
            return "redirect:/dashboard/admin";
        }

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

        // ✅ Lista de perfiles
        model.addAttribute("perfiles", perfilService.findAll());
        model.addAttribute("usuario", updateRequest);

        return "editar";
    }

    // Procesar formulario de edición
    @PostMapping("/editar")
    public String actualizarUsuario(@ModelAttribute UsuarioUpdateRequest request) {
        usuarioService.actualizarUsuario(request);
        return "redirect:/dashboard/admin"; // vuelve al dashboard admin
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
