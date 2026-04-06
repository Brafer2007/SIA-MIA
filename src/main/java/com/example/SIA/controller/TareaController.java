package com.example.SIA.controller;

import com.example.SIA.dto.EntregaResumenDTO;
import com.example.SIA.dto.TareaRequest;
import com.example.SIA.entity.Tarea;
import com.example.SIA.entity.Usuario;
import com.example.SIA.service.TareaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/instructor/tareas")
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    // ---------------------------------------------------------------
    // Helper: obtener idInstructor desde sesión
    // ---------------------------------------------------------------
    private Integer getIdInstructor(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getInstructor() == null) {
            return null;
        }
        return usuario.getInstructor().getId();
    }

    // ---------------------------------------------------------------
    // GET /instructor/tareas — lista tareas del instructor autenticado
    // ---------------------------------------------------------------
    @GetMapping
    public String listarTareas(HttpSession session, Model model) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            return "redirect:/login";
        }

        List<Tarea> tareas = tareaService.listarPorInstructor(idInstructor);
        model.addAttribute("tareas", tareas);
        return "instructor/tareas/lista";
    }

    // ---------------------------------------------------------------
    // GET /instructor/tareas/nueva — formulario de creación
    // ---------------------------------------------------------------
    @GetMapping("/nueva")
    public String formularioNueva(HttpSession session, Model model) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            return "redirect:/login";
        }

        List<String> fichas = tareaService.obtenerFichasDeInstructor(idInstructor);
        model.addAttribute("fichas", fichas);
        model.addAttribute("tareaRequest", new TareaRequest());
        return "instructor/tareas/nueva";
    }

    // ---------------------------------------------------------------
    // POST /instructor/tareas/nueva — guarda tarea
    // ---------------------------------------------------------------
    @PostMapping("/nueva")
    public String guardarTarea(@ModelAttribute TareaRequest tareaRequest,
                               @RequestParam(required = false) MultipartFile archivo,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            return "redirect:/login";
        }

        try {
            tareaService.crearTarea(tareaRequest, idInstructor, archivo);
            redirectAttributes.addFlashAttribute("exito", "Tarea creada correctamente");
            return "redirect:/instructor/tareas";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            List<String> fichas = tareaService.obtenerFichasDeInstructor(idInstructor);
            model.addAttribute("fichas", fichas);
            model.addAttribute("tareaRequest", tareaRequest);
            return "instructor/tareas/nueva";
        }
    }

    // ---------------------------------------------------------------
    // GET /instructor/tareas/{id}/entregas — panel de entregas
    // ---------------------------------------------------------------
    @GetMapping("/{id}/entregas")
    public String verEntregas(@PathVariable Long id,
                              HttpSession session,
                              Model model) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            return "redirect:/login";
        }

        List<EntregaResumenDTO> entregas = tareaService.listarEntregasDeTarea(id, idInstructor);
        model.addAttribute("entregas", entregas);
        model.addAttribute("idTarea", id);
        return "instructor/tareas/entregas";
    }

    // ---------------------------------------------------------------
    // POST /instructor/tareas/{id}/calificar/{idEntrega} — guarda calificación
    // ---------------------------------------------------------------
    @PostMapping("/{id}/calificar/{idEntrega}")
    public String calificar(@PathVariable Long id,
                            @PathVariable Long idEntrega,
                            @RequestParam Double nota,
                            @RequestParam(required = false) String comentario,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            return "redirect:/login";
        }

        try {
            tareaService.calificar(idEntrega, nota, comentario, idInstructor);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/tareas/" + id + "/entregas";
    }

    // ---------------------------------------------------------------
    // REST API — para consumo desde el dashboard vía fetch
    // ---------------------------------------------------------------

    @GetMapping("/api/lista")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiListar(HttpSession session) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();
        List<Map<String, Object>> result = tareaService.listarPorInstructor(idInstructor)
                .stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("titulo", t.getTitulo());
                    m.put("descripcion", t.getDescripcion());
                    m.put("nombreFicha", t.getNombreFicha());
                    m.put("fechaLimite", t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
                    m.put("rutaArchivo", t.getRutaArchivo());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/fichas")
    @ResponseBody
    public ResponseEntity<List<String>> apiFichas(HttpSession session) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(tareaService.obtenerFichasDeInstructor(idInstructor));
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiCrear(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam String fechaLimite,
            @RequestParam String nombreFicha,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            resp.put("error", "No autenticado");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            TareaRequest req = new TareaRequest();
            req.setTitulo(titulo);
            req.setDescripcion(descripcion);
            req.setNombreFicha(nombreFicha);
            // datetime-local puede venir sin segundos: "2025-03-25T14:30"
            java.time.LocalDateTime fechaParsed;
            try {
                fechaParsed = java.time.LocalDateTime.parse(fechaLimite);
            } catch (Exception ex) {
                fechaParsed = java.time.LocalDateTime.parse(fechaLimite,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            }
            req.setFechaLimite(fechaParsed);
            tareaService.crearTarea(req, idInstructor, archivo);
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/api/{id}/entregas")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiEntregas(@PathVariable Long id, HttpSession session) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();
        try {
            List<Map<String, Object>> result = tareaService.listarEntregasDeTarea(id, idInstructor)
                    .stream()
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("idAprendiz", e.getIdAprendiz());
                        m.put("nombreAprendiz", e.getNombreAprendiz());
                        m.put("estadoEntrega", e.getEstadoEntrega());
                        m.put("fechaEntrega", e.getFechaEntrega() != null ? e.getFechaEntrega().toString() : null);
                        m.put("idEntrega", e.getIdEntrega());
                        m.put("rutaArchivo", e.getRutaArchivo());
                        m.put("nota", e.getNota());
                        m.put("comentario", e.getComentario());
                        return m;
                    })
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/api/{id}/calificar/{idEntrega}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiCalificar(
            @PathVariable Long id,
            @PathVariable Long idEntrega,
            @RequestParam Double nota,
            @RequestParam(required = false) String comentario,
            HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            resp.put("error", "No autenticado");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            tareaService.calificar(idEntrega, nota, comentario, idInstructor);
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @DeleteMapping("/api/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiEliminar(
            @PathVariable Long id,
            HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            resp.put("error", "No autenticado");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            tareaService.eliminarTarea(id, idInstructor);
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}
