package com.example.SIA.controller;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.entity.*;
import com.example.SIA.repository.*;
import com.example.SIA.service.ArchivoService;
import com.example.SIA.service.NotificacionService;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/incapacidades")
public class IncapacidadController {

    private static final Logger log = LoggerFactory.getLogger(IncapacidadController.class);

    private final IncapacidadRepository     incapacidadRepository;
    private final AprendizRepository        aprendizRepository;
    private final InstructorRepository      instructorRepository;
    private final AsistenciaRepository      asistenciaRepository;
    private final ProgramacionRepository    programacionRepository;
    private final ArchivoService            archivoService;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificacionService       notificacionService;

    public IncapacidadController(IncapacidadRepository incapacidadRepository,
                                 AprendizRepository aprendizRepository,
                                 InstructorRepository instructorRepository,
                                 AsistenciaRepository asistenciaRepository,
                                 NotificacionRepository notificacionRepository,
                                 ProgramacionRepository programacionRepository,
                                 ArchivoService archivoService,
                                 NotificationWebSocketHandler notificationWebSocketHandler,
                                 NotificacionService notificacionService) {
        this.incapacidadRepository      = incapacidadRepository;
        this.aprendizRepository         = aprendizRepository;
        this.instructorRepository       = instructorRepository;
        this.asistenciaRepository       = asistenciaRepository;
        this.programacionRepository     = programacionRepository;
        this.archivoService             = archivoService;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.notificacionService        = notificacionService;
    }

    // ── Helpers de sesión ────────────────────────────────────────────────────

    private Aprendiz getAprendiz(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null) return null;
        List<Aprendiz> lista = aprendizRepository.findByUsuario_IdUsuario(u.getIdUsuario());
        return lista.isEmpty() ? null : lista.get(0);
    }

    private Integer getIdInstructor(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null || u.getInstructor() == null) return null;
        return u.getInstructor().getId();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── Aprendiz: subir incapacidad ──────────────────────────────────────────

    @PostMapping("/subir")
    @Transactional
    public ResponseEntity<Map<String, Object>> subir(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) {
            resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp);
        }

        Incapacidad inc = new Incapacidad();
        inc.setAprendiz(aprendiz);
        inc.setFechaInicio(LocalDate.parse(fechaInicio));
        inc.setFechaFin(LocalDate.parse(fechaFin));
        inc.setEstado("PENDIENTE");

        if (archivo != null && !archivo.isEmpty()) {
            try {
                archivoService.validarArchivo(archivo, 10 * 1024 * 1024L);
                String ruta = archivoService.guardarArchivoEntrega(
                        (long) aprendiz.getIdAprendiz(), aprendiz.getIdAprendiz(), archivo);
                inc.setRutaArchivo(ruta);
            } catch (IOException e) {
                resp.put("error", "Error al guardar el archivo: " + e.getMessage());
                return ResponseEntity.badRequest().body(resp);
            } catch (IllegalArgumentException e) {
                resp.put("error", e.getMessage());
                return ResponseEntity.badRequest().body(resp);
            }
        }

        incapacidadRepository.save(inc);

        // Notificar a todos los instructores de la ficha
        notificarInstructoresDeFicha(aprendiz, inc);

        resp.put("ok", true);
        return ResponseEntity.ok(resp);
    }

    // ── Aprendiz: ver sus incapacidades ──────────────────────────────────────

    @GetMapping("/mis-incapacidades")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> misIncapacidades(HttpSession session) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                incapacidadRepository.findByAprendiz_IdAprendizOrderByFechaSubidaDesc(aprendiz.getIdAprendiz())
                        .stream().map(i -> toMap(i, false)).collect(Collectors.toList()));
    }

    // ── Instructor: ver incapacidades por ficha ───────────────────────────────

    @GetMapping("/ficha")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> porFicha(
            @RequestParam String ficha, HttpSession session) {
        if (getIdInstructor(session) == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                incapacidadRepository.findByFichaContaining(ficha)
                        .stream().map(i -> toMap(i, true)).collect(Collectors.toList()));
    }

    // ── Instructor: todas las incapacidades de sus fichas ────────────────────

    @GetMapping("/mis-fichas")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> todasMisFichas(HttpSession session) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();

        List<String> fichasInstructor = programacionRepository.findFichasByInstructorId(idInstructor);
        if (fichasInstructor.isEmpty()) return ResponseEntity.ok(List.of());

        return ResponseEntity.ok(
                incapacidadRepository
                        .findAll(org.springframework.data.domain.Sort.by("estado", "fechaSubida"))
                        .stream()
                        .filter(i -> {
                            if (i.getAprendiz() == null) return false;
                            String fichaAp = i.getAprendiz().getFichaFormacion();
                            if (fichaAp == null) return false;
                            return fichasInstructor.stream()
                                    .anyMatch(f -> f.contains(fichaAp) || fichaAp.contains(f));
                        })
                        .map(i -> toMap(i, true))
                        .collect(Collectors.toList()));
    }

    // ── Instructor: aprobar o rechazar ────────────────────────────────────────

    @PostMapping("/{id}/revisar")
    @Transactional
    public ResponseEntity<Map<String, Object>> revisar(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String observacion,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) {
            resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp);
        }
        if (!List.of("APROBADA", "RECHAZADA").contains(decision)) {
            resp.put("error", "Decisión inválida"); return ResponseEntity.badRequest().body(resp);
        }

        Incapacidad inc = incapacidadRepository.findById(id).orElse(null);
        if (inc == null) {
            resp.put("error", "Incapacidad no encontrada"); return ResponseEntity.badRequest().body(resp);
        }

        Instructor instructor = instructorRepository.findById(idInstructor).orElse(null);
        inc.setEstado(decision);
        inc.setInstructorRevisor(instructor);
        inc.setObservacionInstructor(observacion);
        incapacidadRepository.save(inc);

        // Si se aprueba: marcar días como INCAPACIDAD en asistencia
        if ("APROBADA".equals(decision)) {
            LocalDate cursor = inc.getFechaInicio();
            while (!cursor.isAfter(inc.getFechaFin())) {
                RegistroAsistencia reg = asistenciaRepository
                        .findByAprendiz_IdAprendizAndInstructor_IdAndFecha(
                                inc.getAprendiz().getIdAprendiz(), idInstructor, cursor)
                        .orElse(new RegistroAsistencia());
                reg.setAprendiz(inc.getAprendiz());
                reg.setInstructor(instructor);
                reg.setFecha(cursor);
                reg.setEstado("INCAPACIDAD");
                reg.setObservacion("Incapacidad aprobada");
                asistenciaRepository.save(reg);
                cursor = cursor.plusDays(1);
            }
        }

        // Notificar al aprendiz
        notificarAprendizRevision(inc, instructor, decision, observacion);

        resp.put("ok", true);
        resp.put("estado", decision);
        return ResponseEntity.ok(resp);
    }

    // ── Métodos de notificación ───────────────────────────────────────────────

    /**
     * Notifica a todos los instructores de la ficha del aprendiz
     * cuando éste sube una incapacidad.
     */
    private void notificarInstructoresDeFicha(Aprendiz aprendiz, Incapacidad inc) {
        try {
            String ficha = aprendiz.getFichaFormacion();
            if (ficha == null || ficha.isBlank()) return;

            String nombreAprendiz = aprendiz.getUsuario() != null
                    ? (nvl(aprendiz.getUsuario().getNombres()) + " "
                       + nvl(aprendiz.getUsuario().getApellidos())).trim()
                    : "Un aprendiz";

            String titulo = "🏥 Nueva incapacidad de " + nombreAprendiz;
            String mensaje = nombreAprendiz + " subió una incapacidad ("
                    + inc.getFechaInicio() + " al " + inc.getFechaFin() + ")";

            List<Programacion> programaciones = programacionRepository.findByNombreFicha(ficha);
            Set<Integer> notificados = new HashSet<>();

            for (Programacion p : programaciones) {
                if (p.getInstructor() == null) continue;
                if (!notificados.add(p.getInstructor().getId())) continue; // ya notificado

                // WebSocket (tiempo real)
                NotificacionDTO dto = new NotificacionDTO("incapacidad", titulo, mensaje, nombreAprendiz, ficha);
                dto.setSonar(true);
                notificationWebSocketHandler.notificarInstructor(
                        String.valueOf(p.getInstructor().getId()), dto);

                // Persistir para campanita offline
                if (p.getInstructor().getUsuario() != null) {
                    notificacionService.crearParaUsuario(
                            p.getInstructor().getUsuario().getIdUsuario(),
                            "instructor", titulo, mensaje, "incapacidad");
                }
            }

            if (notificados.isEmpty()) {
                log.warn("[Incapacidad] No se encontraron instructores para la ficha '{}'", ficha);
            } else {
                log.info("[Incapacidad] Notificación enviada a {} instructor(es) de la ficha '{}'",
                        notificados.size(), ficha);
            }

        } catch (Exception e) {
            log.error("[Incapacidad] Error al notificar instructores: {}", e.getMessage(), e);
        }
    }

    /**
     * Notifica al aprendiz cuando su incapacidad es aprobada o rechazada.
     */
    private void notificarAprendizRevision(Incapacidad inc, Instructor instructor,
                                           String decision, String observacion) {
        try {
            Aprendiz aprendiz = inc.getAprendiz();
            if (aprendiz == null || aprendiz.getUsuario() == null) return;

            String nombreInstructor = instructor != null ? instructor.getNombreCompleto() : "Instructor";
            String ficha = aprendiz.getFichaFormacion();
            boolean aprobada = "APROBADA".equals(decision);

            String titulo = aprobada ? "✅ Incapacidad aprobada" : "❌ Incapacidad rechazada";
            String mensaje = aprobada
                    ? "Tu incapacidad del " + inc.getFechaInicio() + " al " + inc.getFechaFin()
                      + " fue aprobada por " + nombreInstructor + "."
                    : "Tu incapacidad del " + inc.getFechaInicio() + " al " + inc.getFechaFin()
                      + " fue rechazada por " + nombreInstructor + "."
                      + (observacion != null && !observacion.isBlank() ? " Motivo: " + observacion : "");

            // WebSocket (tiempo real, si el aprendiz está conectado)
            NotificacionDTO dto = new NotificacionDTO("incapacidad", titulo, mensaje, nombreInstructor, ficha);
            dto.setSonar(true);
            notificationWebSocketHandler.notificarAprendicesDeFicha(ficha, dto);

            // Persistir en BD para campanita offline
            notificacionService.crearParaUsuario(
                    aprendiz.getUsuario().getIdUsuario(),
                    "aprendiz", titulo, mensaje, "incapacidad");

            log.info("[Incapacidad] Notificación '{}' enviada al aprendiz id={}",
                    decision, aprendiz.getIdAprendiz());

        } catch (Exception e) {
            log.error("[Incapacidad] Error al notificar aprendiz: {}", e.getMessage(), e);
        }
    }

    // ── Helper de mapeo ───────────────────────────────────────────────────────

    private Map<String, Object> toMap(Incapacidad i, boolean incluirAprendiz) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.getId());
        m.put("fechaInicio", i.getFechaInicio().toString());
        m.put("fechaFin", i.getFechaFin().toString());
        m.put("estado", i.getEstado());
        m.put("rutaArchivo", i.getRutaArchivo());
        m.put("observacionInstructor", i.getObservacionInstructor());
        m.put("fechaSubida", i.getFechaSubida() != null ? i.getFechaSubida().toString() : null);
        if (incluirAprendiz && i.getAprendiz() != null && i.getAprendiz().getUsuario() != null) {
            String nombre = (nvl(i.getAprendiz().getUsuario().getNombres()) + " "
                    + nvl(i.getAprendiz().getUsuario().getApellidos())).trim();
            m.put("nombreAprendiz", nombre);
            m.put("idAprendiz", i.getAprendiz().getIdAprendiz());
        }
        return m;
    }
}
