package com.example.SIA.controller;

import com.example.SIA.dto.EnviarCorreoDTO;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.ProgramacionRepository;
import com.example.SIA.service.EmailService;
import com.example.SIA.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired private EmailService emailService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private AprendizRepository aprendizRepository;
    @Autowired private ProgramacionRepository programacionRepository;

    // ─── Envío masivo ────────────────────────────────────────────────────────────

    @PostMapping("/enviar-correos")
    public ResponseEntity<Map<String, Object>> enviarCorreos(
            @RequestBody EnviarCorreoDTO request, HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        try {
            Integer idUsuario = (Integer) session.getAttribute("idUsuario");
            Integer perfil    = (Integer) session.getAttribute("id_perfil");
            if (idUsuario == null || perfil == null || perfil != 3) {
                resp.put("exito", false);
                resp.put("mensaje", "Acceso denegado.");
                return ResponseEntity.status(403).body(resp);
            }

            String emailInstructor = usuarioService.findById(idUsuario).getCorreo();

            if (request.getDestinatarios() == null || request.getDestinatarios().isEmpty()) {
                resp.put("exito", false); resp.put("mensaje", "Selecciona al menos un destinatario.");
                return ResponseEntity.badRequest().body(resp);
            }
            if (request.getAsunto() == null || request.getAsunto().isBlank()) {
                resp.put("exito", false); resp.put("mensaje", "El asunto no puede estar vacío.");
                return ResponseEntity.badRequest().body(resp);
            }
            if (request.getMensaje() == null || request.getMensaje().isBlank()) {
                resp.put("exito", false); resp.put("mensaje", "El mensaje no puede estar vacío.");
                return ResponseEntity.badRequest().body(resp);
            }

            boolean enviado = emailService.enviarCorreoMasivo(
                    request.getDestinatarios(), request.getAsunto(),
                    request.getMensaje(), emailInstructor);

            if (enviado) {
                resp.put("exito", true);
                resp.put("mensaje", "Correos enviados a " + request.getDestinatarios().size() + " destinatario(s).");
                resp.put("cantidad", request.getDestinatarios().size());
                logger.info("Instructor {} envió correo masivo a {} destinatarios", idUsuario, request.getDestinatarios().size());
                return ResponseEntity.ok(resp);
            }
            resp.put("exito", false); resp.put("mensaje", "Error al enviar. Intenta de nuevo.");
            return ResponseEntity.status(500).body(resp);

        } catch (Exception e) {
            logger.error("Error envío correos: {}", e.getMessage(), e);
            resp.put("exito", false); resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    // ─── Aprendices de una ficha (con correo) ────────────────────────────────────

    /**
     * Devuelve los aprendices que pertenecen a la ficha indicada, con su nombre y correo.
     * El instructor selecciona la ficha y el frontend pre-llena los destinatarios.
     */
    @GetMapping("/aprendices-por-ficha")
    public ResponseEntity<Map<String, Object>> aprendicesPorFicha(
            @RequestParam String ficha, HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        try {
            Integer perfil = (Integer) session.getAttribute("id_perfil");
            if (perfil == null || perfil != 3) {
                resp.put("exito", false); resp.put("mensaje", "Acceso denegado.");
                return ResponseEntity.status(403).body(resp);
            }

            List<Aprendiz> aprendices = aprendizRepository.findByFichaContainedIn(ficha);
            List<Map<String, String>> lista = aprendices.stream()
                    .filter(a -> a.getUsuario() != null
                              && a.getUsuario().getCorreo() != null
                              && !a.getUsuario().getCorreo().isBlank())
                    .map(a -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("email", a.getUsuario().getCorreo());
                        m.put("nombre", (a.getUsuario().getNombres() + " " + a.getUsuario().getApellidos()).trim());
                        return m;
                    })
                    .collect(Collectors.toList());

            resp.put("exito", true);
            resp.put("aprendices", lista);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("Error obteniendo aprendices por ficha: {}", e.getMessage(), e);
            resp.put("exito", false); resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    // ─── Fichas del instructor ────────────────────────────────────────────────────

    @GetMapping("/fichas-instructor")
    public ResponseEntity<Map<String, Object>> fichasInstructor(HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Integer perfil  = (Integer) session.getAttribute("id_perfil");
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (perfil == null || perfil != 3 || usuario == null || usuario.getInstructor() == null) {
                resp.put("exito", false); resp.put("mensaje", "Acceso denegado.");
                return ResponseEntity.status(403).body(resp);
            }
            List<String> fichas = programacionRepository
                    .findFichasByInstructorId(usuario.getInstructor().getId());
            resp.put("exito", true);
            resp.put("fichas", fichas);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("exito", false); resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }
}
