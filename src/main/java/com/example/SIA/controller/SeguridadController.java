package com.example.SIA.controller;

import com.example.SIA.entity.Instructor;
import com.example.SIA.entity.RegistroAcceso;
import com.example.SIA.entity.SolicitudAmbiente;
import com.example.SIA.entity.Usuario;
import com.example.SIA.entity.Equipo;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.service.EquipoService;
import com.example.SIA.repository.InstructorRepository;
import com.example.SIA.repository.RegistroAccesoRepository;
import com.example.SIA.repository.SolicitudAmbienteRepository;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.dto.SolicitudAmbienteDTO;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/seguridad")
public class SeguridadController {

    private final UsuarioService usuarioService;
    private final EquipoService equipoService;
    private final RegistroAccesoRepository registroAccesoRepository;
    private final SolicitudAmbienteRepository solicitudAmbienteRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final InstructorRepository instructorRepository;

    public SeguridadController(UsuarioService usuarioService,
            EquipoService equipoService,
            RegistroAccesoRepository registroAccesoRepository,
            SolicitudAmbienteRepository solicitudAmbienteRepository,
            NotificationWebSocketHandler notificationWebSocketHandler,
            InstructorRepository instructorRepository) {
        this.usuarioService = usuarioService;
        this.equipoService = equipoService;
        this.registroAccesoRepository = registroAccesoRepository;
        this.solicitudAmbienteRepository = solicitudAmbienteRepository;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.instructorRepository = instructorRepository;
    }

    // ==========================================
    // VISTA DEL DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    public String dashboardSeguridad(HttpSession session, Model model) {
        Integer perfil = (Integer) session.getAttribute("id_perfil");
        if (perfil == null) {
            return "redirect:/login";
        }
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Usuario usuario = usuarioService.findById(idUsuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("idUsuario", idUsuario);
        return "dashboardSeguridad";
    }

    // ==========================================
    // API: BUSCAR USUARIO Y SUS EQUIPOS (QR/Cédula)
    // ==========================================
    @GetMapping("/api/lookup")
    @ResponseBody
    public ResponseEntity<?> buscarUsuario(@RequestParam String documento) {
        Usuario usuario = usuarioService.findByNoDocumento(documento);
        if (usuario == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado con documento: " + documento));
        }

        List<Equipo> equipos = equipoService.listarPorUsuario(usuario.getIdUsuario());

        Optional<RegistroAcceso> ultimoRegistro = registroAccesoRepository
                .findTopByUsuario_IdUsuarioOrderByFechaHoraDesc(usuario.getIdUsuario());

        boolean estaAdentro = ultimoRegistro.isPresent() && "INGRESO".equals(ultimoRegistro.get().getTipo());
        String equiposConQueIngreso = estaAdentro ? ultimoRegistro.get().getEquiposIngresados() : null;

        Map<String, Object> response = new HashMap<>();
        response.put("idUsuario", usuario.getIdUsuario());
        response.put("nombres", usuario.getNombres());
        response.put("apellidos", usuario.getApellidos());
        response.put("noDocumento", usuario.getNoDocumento());
        String rol = usuario.getPerfil() != null ? usuario.getPerfil().getNombrePerfil() : "Usuario";
        response.put("rol", rol);
        response.put("equipos", equipos);
        response.put("estaAdentro", estaAdentro);
        response.put("equiposConQueIngreso", equiposConQueIngreso != null ? equiposConQueIngreso : "");

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // API: REGISTRAR INGRESO / SALIDA
    // ==========================================
    @PostMapping("/api/registro")
    @ResponseBody
    public ResponseEntity<?> registrarAcceso(@RequestBody Map<String, Object> payload) {
        try {
            Integer idUsuario = Integer.parseInt(payload.get("idUsuario").toString());
            String tipo = payload.get("tipo").toString();
            String metodo = payload.get("metodo").toString();
            String equiposIngresados = payload.containsKey("equipos") ? payload.get("equipos").toString() : "";

            Usuario usuario = usuarioService.findById(idUsuario);
            if (usuario == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
            }

            RegistroAcceso registro = new RegistroAcceso();
            registro.setUsuario(usuario);
            registro.setTipo(tipo);
            registro.setMetodo(metodo);
            registro.setEquiposIngresados(equiposIngresados);
            registro.setFechaHora(LocalDateTime.now());
            registroAccesoRepository.save(registro);

            return ResponseEntity.ok(Map.of("mensaje", "Registro de " + tipo + " exitoso", "fecha", registro.getFechaHora()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error procesando el registro: " + e.getMessage()));
        }
    }

    // ==========================================
    // API: GESTIÓN DE AMBIENTES
    // ==========================================
    @GetMapping("/api/solicitudes")
    @ResponseBody
    public ResponseEntity<List<SolicitudAmbienteDTO>> listarSolicitudesPendientes() {
        List<SolicitudAmbienteDTO> pendientes = solicitudAmbienteRepository
                .findByEstadoOrderByFechaSolicitudDesc("PENDIENTE")
                .stream().map(SolicitudAmbienteDTO::from).toList();
        return ResponseEntity.ok(pendientes);
    }

    @PostMapping("/api/solicitud")
    @ResponseBody
    public ResponseEntity<?> enviarSolicitud(@RequestBody Map<String, Object> payload) {
        try {
            Integer idUsuario = Integer.parseInt(payload.get("idUsuario").toString());
            String aula = payload.get("aula").toString();
            String tipo = payload.get("tipo").toString();

            Instructor instructor = instructorRepository.findByUsuario_IdUsuario(idUsuario).orElse(null);
            if (instructor == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El usuario no tiene perfil de instructor asociado"));
            }

            SolicitudAmbiente sol = new SolicitudAmbiente();
            sol.setInstructor(instructor);
            sol.setAula(aula);
            sol.setTipo(tipo);
            sol.setEstado("PENDIENTE");
            sol.setFechaSolicitud(LocalDateTime.now());
            solicitudAmbienteRepository.save(sol);

            String nombre = instructor.getUsuario() != null ? instructor.getUsuario().getNombres() : "Instructor";
            NotificacionDTO msg = new NotificacionDTO("solicitud_ambiente", "Solicitud de Aula",
                    "El instructor " + nombre + " solicita " + tipo + " del aula " + aula, "Sistema", "N/A");
            msg.setIdMensaje(sol.getId());
            notificationWebSocketHandler.notificarSeguridad(msg);

            return ResponseEntity.ok(Map.of("mensaje", "Solicitud enviada correctamente a seguridad."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno al enviar solicitud: " + e.getMessage()));
        }
    }

    // Convierte una solicitud APROBADA de APERTURA en solicitud de CIERRE (sin crear nueva fila)
    @PostMapping("/api/solicitud/{id}/solicitar-cierre")
    @ResponseBody
    public ResponseEntity<?> solicitarCierre(@PathVariable Long id) {
        Optional<SolicitudAmbiente> opt = solicitudAmbienteRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solicitud no encontrada"));
        }
        SolicitudAmbiente sol = opt.get();
        if (!"APROBADA".equals(sol.getEstado()) || !"APERTURA".equals(sol.getTipo())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se puede cerrar un ambiente abierto y aprobado"));
        }
        sol.setTipo("CIERRE");
        sol.setEstado("PENDIENTE");
        sol.setFechaSolicitud(LocalDateTime.now());
        solicitudAmbienteRepository.save(sol);

        String nombre = sol.getInstructor() != null && sol.getInstructor().getUsuario() != null
                ? sol.getInstructor().getUsuario().getNombres() : "Instructor";
        NotificacionDTO msg = new NotificacionDTO("solicitud_ambiente", "Solicitud de Cierre",
                "El instructor " + nombre + " solicita CIERRE del aula " + sol.getAula(), "Sistema", "N/A");
        msg.setIdMensaje(sol.getId());
        notificationWebSocketHandler.notificarSeguridad(msg);

        return ResponseEntity.ok(Map.of("mensaje", "Solicitud de cierre enviada.",
                "id", sol.getId(), "tipo", sol.getTipo(), "estado", sol.getEstado()));
    }

    @PostMapping("/api/solicitudes/{id}/estado")
    @ResponseBody
    public ResponseEntity<?> actualizarEstadoSolicitud(@PathVariable Long id, @RequestParam String estado) {
        Optional<SolicitudAmbiente> opt = solicitudAmbienteRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solicitud no encontrada"));
        }
        SolicitudAmbiente sol = opt.get();
        sol.setEstado(estado);
        solicitudAmbienteRepository.save(sol);

        NotificacionDTO msg = new NotificacionDTO("estado_solicitud", "Actualización de Solicitud de Aula",
                "Tu solicitud para el aula " + sol.getAula() + " ha sido " + estado, "Seguridad", "N/A");
        msg.setIdMensaje(sol.getId());
        msg.setSala(estado);
        notificationWebSocketHandler.notificarInstructor(String.valueOf(sol.getInstructor().getId()), msg);

        return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado a " + estado));
    }

    // ==========================================
    // API: HISTORIAL DE SOLICITUDES DEL INSTRUCTOR
    // ==========================================
    @GetMapping("/api/solicitudes/instructor/{idInstructor}")
    @ResponseBody
    public ResponseEntity<List<SolicitudAmbienteDTO>> listarSolicitudesInstructor(@PathVariable Integer idInstructor) {
        List<SolicitudAmbienteDTO> historial = solicitudAmbienteRepository
                .findByInstructor_IdOrderByFechaSolicitudDesc(idInstructor)
                .stream().map(SolicitudAmbienteDTO::from).toList();
        return ResponseEntity.ok(historial);
    }

    // ==========================================
    // API: REPORTE DIARIO DE ACCESOS
    // ==========================================
    @GetMapping("/api/reporte-diario")
    @ResponseBody
    public ResponseEntity<?> reporteDiario(@RequestParam String fecha) {
        try {
            java.time.LocalDate dia = java.time.LocalDate.parse(fecha);
            java.time.LocalDateTime inicio = dia.atStartOfDay();
            java.time.LocalDateTime fin = dia.atTime(23, 59, 59);

            List<RegistroAcceso> registros = registroAccesoRepository.findByFechaHoraBetween(inicio, fin);

            long ingresos = registros.stream().filter(r -> "INGRESO".equals(r.getTipo())).count();
            long salidas = registros.stream().filter(r -> "SALIDA".equals(r.getTipo())).count();

            List<Map<String, Object>> detalle = registros.stream().map(r -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("hora", r.getFechaHora().toLocalTime().toString().substring(0, 5));
                m.put("nombre", r.getUsuario().getNombres() + " " + r.getUsuario().getApellidos());
                m.put("documento", r.getUsuario().getNoDocumento());
                m.put("tipo", r.getTipo());
                m.put("metodo", r.getMetodo());
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of("fecha", fecha, "totalIngresos", ingresos,
                    "totalSalidas", salidas, "detalle", detalle));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}
