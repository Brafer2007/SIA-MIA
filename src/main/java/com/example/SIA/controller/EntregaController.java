package com.example.SIA.controller;

import com.example.SIA.dto.TareaAprendizDTO;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.service.EntregaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/aprendiz/tareas")
public class EntregaController {

    private final EntregaService entregaService;
    private final AprendizRepository aprendizRepository;

    public EntregaController(EntregaService entregaService,
                             AprendizRepository aprendizRepository) {
        this.entregaService = entregaService;
        this.aprendizRepository = aprendizRepository;
    }

    // ---------------------------------------------------------------
    // Helper: obtener Aprendiz desde sesión
    // ---------------------------------------------------------------
    private Aprendiz getAprendiz(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return null;
        }
        List<Aprendiz> aprendices = aprendizRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        return aprendices.isEmpty() ? null : aprendices.get(0);
    }

    // ---------------------------------------------------------------
    // GET /aprendiz/tareas — lista tareas de la ficha del aprendiz
    // ---------------------------------------------------------------
    @GetMapping
    public String listarTareas(HttpSession session, Model model) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) {
            return "redirect:/login";
        }

        List<TareaAprendizDTO> tareas = entregaService.listarTareasParaAprendiz(
                aprendiz.getFichaFormacion(), aprendiz.getIdAprendiz());
        model.addAttribute("tareas", tareas);
        return "aprendiz/tareas/lista";
    }

    // ---------------------------------------------------------------
    // GET /aprendiz/tareas/{id} — detalle de tarea + formulario de entrega
    // ---------------------------------------------------------------
    @GetMapping("/{id}")
    public String detalleTarea(@PathVariable Long id,
                               HttpSession session,
                               Model model) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) {
            return "redirect:/login";
        }

        List<TareaAprendizDTO> tareas = entregaService.listarTareasParaAprendiz(
                aprendiz.getFichaFormacion(), aprendiz.getIdAprendiz());

        TareaAprendizDTO tarea = tareas.stream()
                .filter(t -> t.getIdTarea().equals(id))
                .findFirst()
                .orElse(null);

        if (tarea == null) {
            return "redirect:/aprendiz/tareas";
        }

        model.addAttribute("tarea", tarea);
        return "aprendiz/tareas/detalle";
    }

    // ---------------------------------------------------------------
    // POST /aprendiz/tareas/{id}/entregar — sube entrega
    // ---------------------------------------------------------------
    @PostMapping("/{id}/entregar")
    public String entregar(@PathVariable Long id,
                           @RequestParam MultipartFile archivo,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) {
            return "redirect:/login";
        }

        try {
            entregaService.entregar(id, aprendiz.getIdAprendiz(), archivo);
            redirectAttributes.addFlashAttribute("exito", "Entrega realizada correctamente");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "redirect:/aprendiz/tareas/" + id;
    }

    // ---------------------------------------------------------------
    // REST API — para consumo desde el dashboard del aprendiz vía fetch
    // ---------------------------------------------------------------

    @GetMapping("/api/lista")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiListar(HttpSession session) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) return ResponseEntity.status(401).build();
        String ficha = aprendiz.getFichaFormacion();
        if (ficha == null || ficha.isBlank()) return ResponseEntity.ok(java.util.List.of());
        List<Map<String, Object>> result = entregaService.listarTareasParaAprendiz(ficha, aprendiz.getIdAprendiz())
                .stream()
                .map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idTarea", t.getIdTarea());
                    m.put("titulo", t.getTitulo());
                    m.put("descripcion", t.getDescripcion());
                    m.put("nombreInstructor", t.getNombreInstructor());
                    m.put("fechaLimite", t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
                    m.put("estadoEntrega", t.getEstadoEntrega());
                    m.put("nota", t.getNota());
                    m.put("comentarioInstructor", t.getComentarioInstructor());
                    m.put("tieneArchivoTarea", t.isTieneArchivoTarea());
                    m.put("rutaArchivoTarea", t.getRutaArchivoTarea());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/{id}/entregar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiEntregar(
            @PathVariable Long id,
            @RequestParam MultipartFile archivo,
            HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) {
            resp.put("error", "No autenticado");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            entregaService.entregar(id, aprendiz.getIdAprendiz(), archivo);
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}
