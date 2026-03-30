package com.example.SIA.controller;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.entity.*;
import com.example.SIA.repository.*;
import com.example.SIA.service.ArchivoService;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final IncapacidadRepository incapacidadRepository;
    private final AprendizRepository aprendizRepository;
    private final InstructorRepository instructorRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final NotificacionRepository notificacionRepository;
    private final ProgramacionRepository programacionRepository;
    private final ArchivoService archivoService;

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    public IncapacidadController(IncapacidadRepository incapacidadRepository,
                                 AprendizRepository aprendizRepository,
                                 InstructorRepository instructorRepository,
                                 AsistenciaRepository asistenciaRepository,
                                 NotificacionRepository notificacionRepository,
                                 ProgramacionRepository programacionRepository,
                                 ArchivoService archivoService) {
        this.incapacidadRepository = incapacidadRepository;
        this.aprendizRepository = aprendizRepository;
        this.instructorRepository = instructorRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.notificacionRepository = notificacionRepository;
        this.programacionRepository = programacionRepository;
        this.archivoService = archivoService;
    }

    private Aprendiz getAprendiz(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null) return null;
        java.util.List<Aprendiz> lista = aprendizRepository.findByUsuario_IdUsuario(u.getIdUsuario());
        return lista.isEmpty() ? null : lista.get(0);
    }

    private Integer getIdInstructor(HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null || u.getInstructor() == null) return null;
        return u.getInstructor().getId();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ---------------------------------------------------------------
    // Aprendiz: subir incapacidad
    // ---------------------------------------------------------------
    @PostMapping("/subir")
    @Transactional
    public ResponseEntity<Map<String, Object>> subir(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(required = false) MultipartFile archivo,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) { resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp); }

        Incapacidad inc = new Incapacidad();
        inc.setAprendiz(aprendiz);
        inc.setFechaInicio(LocalDate.parse(fechaInicio));
        inc.setFechaFin(LocalDate.parse(fechaFin));
        inc.setEstado("PENDIENTE");

        if (archivo != null && !archivo.isEmpty()) {
            try {
                archivoService.validarArchivo(archivo, 10 * 1024 * 1024L);
                // Guardar en uploads/incapacidades/{idAprendiz}/
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
        resp.put("ok", true);
        return ResponseEntity.ok(resp);
    }

    // ---------------------------------------------------------------
    // Aprendiz: ver sus incapacidades
    // ---------------------------------------------------------------
    @GetMapping("/mis-incapacidades")
    @Transactional
    public ResponseEntity<java.util.List<Map<String, Object>>> misIncapacidades(HttpSession session) {
        Aprendiz aprendiz = getAprendiz(session);
        if (aprendiz == null) return ResponseEntity.status(401).build();

        java.util.List<Map<String, Object>> result = incapacidadRepository
                .findByAprendiz_IdAprendizOrderByFechaSubidaDesc(aprendiz.getIdAprendiz())
                .stream().map(i -> toMap(i, false)).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------
    // Instructor: ver incapacidades de su ficha
    // ---------------------------------------------------------------
    @GetMapping("/ficha")
    @Transactional
    public ResponseEntity<java.util.List<Map<String, Object>>> porFicha(
            @RequestParam String ficha,
            HttpSession session) {

        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();

        java.util.List<Map<String, Object>> result = incapacidadRepository
                .findByFichaContaining(ficha)
                .stream().map(i -> toMap(i, true)).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------
    // Instructor: todas las incapacidades de todas sus fichas
    // ---------------------------------------------------------------
    @GetMapping("/mis-fichas")
    @Transactional
    public ResponseEntity<java.util.List<Map<String, Object>>> todasMisFichas(HttpSession session) {
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();

        // Fichas del instructor (pueden ser rangos: "2996893 - 2996900")
        java.util.List<String> fichasInstructor = programacionRepository.findFichasByInstructorId(idInstructor);
        if (fichasInstructor.isEmpty()) return ResponseEntity.ok(java.util.List.of());

        // Traer todas las incapacidades y filtrar en Java:
        // una incapacidad pertenece al instructor si alguna de sus fichas CONTIENE
        // la fichaFormacion del aprendiz (ej: "2996893 - 2996900" contiene "2996893")
        java.util.List<Map<String, Object>> result = incapacidadRepository
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
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------
    // Instructor: aprobar o rechazar incapacidad
    // ---------------------------------------------------------------
    @PostMapping("/{id}/revisar")
    @Transactional
    public ResponseEntity<Map<String, Object>> revisar(
            @PathVariable Long id,
            @RequestParam String decision,   // APROBADA o RECHAZADA
            @RequestParam(required = false) String observacion,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) { resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp); }
        if (!java.util.List.of("APROBADA", "RECHAZADA").contains(decision)) {
            resp.put("error", "Decisión inválida"); return ResponseEntity.badRequest().body(resp);
        }

        Incapacidad inc = incapacidadRepository.findById(id).orElse(null);
        if (inc == null) { resp.put("error", "Incapacidad no encontrada"); return ResponseEntity.badRequest().body(resp); }

        Instructor instructor = instructorRepository.findById(idInstructor).orElse(null);
        inc.setEstado(decision);
        inc.setInstructorRevisor(instructor);
        inc.setObservacionInstructor(observacion);
        incapacidadRepository.save(inc);

        // Si se aprueba: marcar cada día del rango como INCAPACIDAD en asistencia
        if ("APROBADA".equals(decision)) {
            LocalDate cursor = inc.getFechaInicio();
            while (!cursor.isAfter(inc.getFechaFin())) {
                final LocalDate dia = cursor;
                RegistroAsistencia reg = asistenciaRepository
                        .findByAprendiz_IdAprendizAndInstructor_IdAndFecha(
                                inc.getAprendiz().getIdAprendiz(), idInstructor, dia)
                        .orElse(new RegistroAsistencia());
                reg.setAprendiz(inc.getAprendiz());
                reg.setInstructor(instructor);
                reg.setFecha(dia);
                reg.setEstado("INCAPACIDAD");
                reg.setObservacion("Incapacidad aprobada");
                asistenciaRepository.save(reg);
                cursor = cursor.plusDays(1);
            }

            // Notificar al aprendiz
            String ficha = inc.getAprendiz().getFichaFormacion();
            String nombreInstructor = instructor != null ? instructor.getNombreCompleto() : "Instructor";
            NotificacionDTO dto = new NotificacionDTO("incapacidad_aprobada",
                    "✅ Incapacidad aprobada",
                    "Tu incapacidad del " + inc.getFechaInicio() + " al " + inc.getFechaFin()
                            + " fue aprobada por " + nombreInstructor,
                    nombreInstructor, ficha);
            dto.setSonar(true);
            notificationWebSocketHandler.notificarAprendicesDeFicha(ficha, dto);

            Notificacion notif = new Notificacion();
            notif.setMensaje("Incapacidad aprobada del " + inc.getFechaInicio() + " al " + inc.getFechaFin());
            notif.setTipo("incapacidad_aprobada");
            notif.setCategoria("asistencia");
            notif.setPrioridad("media");
            notificacionRepository.save(notif);
        } else {
            // Notificar rechazo
            String ficha = inc.getAprendiz().getFichaFormacion();
            String nombreInstructor = instructor != null ? instructor.getNombreCompleto() : "Instructor";
            NotificacionDTO dto = new NotificacionDTO("incapacidad_rechazada",
                    "❌ Incapacidad rechazada",
                    "Tu incapacidad del " + inc.getFechaInicio() + " al " + inc.getFechaFin()
                            + " fue rechazada. " + nvl(observacion),
                    nombreInstructor, ficha);
            dto.setSonar(true);
            notificationWebSocketHandler.notificarAprendicesDeFicha(ficha, dto);
        }

        resp.put("ok", true);
        resp.put("estado", decision);
        return ResponseEntity.ok(resp);
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private Map<String, Object> toMap(Incapacidad i, boolean incluirAprendiz) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.getId());
        m.put("fechaInicio", i.getFechaInicio().toString());
        m.put("fechaFin", i.getFechaFin().toString());
        m.put("estado", i.getEstado());
        m.put("rutaArchivo", i.getRutaArchivo());
        m.put("observacionInstructor", i.getObservacionInstructor());
        m.put("fechaSubida", i.getFechaSubida() != null ? i.getFechaSubida().toString() : null);
        if (incluirAprendiz && i.getAprendiz() != null) {
            String nombre = i.getAprendiz().getUsuario() != null
                    ? (nvl(i.getAprendiz().getUsuario().getNombres()) + " "
                       + nvl(i.getAprendiz().getUsuario().getApellidos())).trim()
                    : "";
            m.put("nombreAprendiz", nombre);
            m.put("idAprendiz", i.getAprendiz().getIdAprendiz());
        }
        return m;
    }
}
