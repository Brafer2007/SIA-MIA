package com.example.SIA.controller;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Instructor;
import com.example.SIA.entity.RegistroAsistencia;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.AsistenciaRepository;
import com.example.SIA.repository.InstructorRepository;
import com.example.SIA.service.EmailService;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/asistencia")
public class AsistenciaController {

    private final AsistenciaRepository asistenciaRepository;
    private final AprendizRepository aprendizRepository;
    private final InstructorRepository instructorRepository;

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.example.SIA.service.NotificacionService notificacionService;

    public AsistenciaController(AsistenciaRepository asistenciaRepository,
                                AprendizRepository aprendizRepository,
                                InstructorRepository instructorRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.aprendizRepository = aprendizRepository;
        this.instructorRepository = instructorRepository;
    }

    private Integer getIdInstructor(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getInstructor() == null) return null;
        return usuario.getInstructor().getId();
    }

    private Aprendiz getAprendizFromSession(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return null;
        java.util.List<Aprendiz> lista = aprendizRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        return lista.isEmpty() ? null : lista.get(0);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ---------------------------------------------------------------
    // Aprendices de una ficha con estado del día
    // ---------------------------------------------------------------
    @GetMapping("/ficha")
    @Transactional
    public ResponseEntity<java.util.List<Map<String, Object>>> getAprendicesFicha(
            @RequestParam String ficha,
            @RequestParam(required = false) String fecha,
            HttpSession session) {

        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) return ResponseEntity.status(401).build();

        LocalDate dia = (fecha != null && !fecha.isBlank()) ? LocalDate.parse(fecha) : LocalDate.now();
        java.util.List<Aprendiz> aprendices = aprendizRepository.findByFichaContainedIn(ficha);

        java.util.List<Map<String, Object>> result = aprendices.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("idAprendiz", a.getIdAprendiz());
            String nombre = a.getUsuario() != null
                    ? (nvl(a.getUsuario().getNombres()) + " " + nvl(a.getUsuario().getApellidos())).trim() : "";
            m.put("nombre", nombre);
            m.put("fichaFormacion", a.getFichaFormacion());
            asistenciaRepository.findByAprendiz_IdAprendizAndInstructor_IdAndFecha(
                    a.getIdAprendiz(), idInstructor, dia)
                    .ifPresentOrElse(r -> {
                        m.put("estado", r.getEstado());
                        m.put("observacion", r.getObservacion());
                        m.put("idRegistro", r.getId());
                    }, () -> {
                        m.put("estado", null);
                        m.put("observacion", null);
                        m.put("idRegistro", null);
                    });
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------
    // Guardar asistencia
    // ---------------------------------------------------------------
    @PostMapping("/guardar")
    @Transactional
    public ResponseEntity<Map<String, Object>> guardar(
            @RequestParam Integer idAprendiz,
            @RequestParam String estado,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) String fecha,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) { resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp); }
        if (!java.util.List.of("PRESENTE", "RETRASO", "AUSENTE", "INCAPACIDAD").contains(estado)) {
            resp.put("error", "Estado inválido"); return ResponseEntity.badRequest().body(resp);
        }

        LocalDate dia = (fecha != null && !fecha.isBlank()) ? LocalDate.parse(fecha) : LocalDate.now();
        Aprendiz aprendiz = aprendizRepository.findById(idAprendiz).orElse(null);
        Instructor instructor = instructorRepository.findById(idInstructor).orElse(null);
        if (aprendiz == null || instructor == null) {
            resp.put("error", "Datos no encontrados"); return ResponseEntity.badRequest().body(resp);
        }

        RegistroAsistencia registro = asistenciaRepository
                .findByAprendiz_IdAprendizAndInstructor_IdAndFecha(idAprendiz, idInstructor, dia)
                .orElse(new RegistroAsistencia());
        registro.setAprendiz(aprendiz);
        registro.setInstructor(instructor);
        registro.setFecha(dia);
        registro.setEstado(estado);
        registro.setObservacion(observacion);
        asistenciaRepository.save(registro);

        // Enviar correo automático si es AUSENTE o INCAPACIDAD
        if (("AUSENTE".equals(estado) || "INCAPACIDAD".equals(estado))
                && aprendiz.getUsuario() != null
                && aprendiz.getUsuario().getCorreo() != null
                && !aprendiz.getUsuario().getCorreo().isBlank()) {

            String nombreAprendiz = (aprendiz.getUsuario().getNombres()
                    + " " + aprendiz.getUsuario().getApellidos()).trim();
            String nombreInstructor = instructor.getNombreCompleto();
            String diaStr = dia.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String estadoTexto = "AUSENTE".equals(estado) ? "Inasistencia" : "Incapacidad";

            String asunto = "⚠️ " + estadoTexto + " registrada — " + diaStr;
            String cuerpo = "Hola " + nombreAprendiz + ",\n\n"
                    + "Tu instructor " + nombreInstructor + " registró una " + estadoTexto.toLowerCase()
                    + " para el día " + diaStr + ".\n\n"
                    + (observacion != null && !observacion.isBlank()
                        ? "Observación: " + observacion + "\n\n" : "")
                    + "Si crees que esto es un error, comunícate con tu instructor.\n\n"
                    + "— Sistema SIA";

            String correoInstructor = instructor.getUsuario() != null
                    ? instructor.getUsuario().getCorreo() : null;
            emailService.enviarCorreoIndividual(
                    aprendiz.getUsuario().getCorreo(), asunto, cuerpo, correoInstructor);

            // Persistir para campanita offline
            if (aprendiz.getUsuario().getIdUsuario() != null) {
                notificacionService.crearParaUsuario(
                        aprendiz.getUsuario().getIdUsuario(), "aprendiz",
                        asunto,
                        (observacion != null && !observacion.isBlank() ? observacion : estadoTexto + " el " + diaStr),
                        "inasistencia");
            }
        }

        resp.put("ok", true);
        resp.put("estado", estado);
        return ResponseEntity.ok(resp);
    }

    // ---------------------------------------------------------------
    // Notificar inasistencia al aprendiz
    // ---------------------------------------------------------------
    @PostMapping("/notificar-inasistencia")
    @Transactional
    public ResponseEntity<Map<String, Object>> notificarInasistencia(
            @RequestParam Integer idAprendiz,
            @RequestParam(required = false) String fecha,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) { resp.put("error", "No autenticado"); return ResponseEntity.status(401).body(resp); }

        Aprendiz aprendiz = aprendizRepository.findById(idAprendiz).orElse(null);
        if (aprendiz == null) { resp.put("error", "Aprendiz no encontrado"); return ResponseEntity.badRequest().body(resp); }

        Instructor instructor = instructorRepository.findById(idInstructor).orElse(null);
        String nombreInstructor = instructor != null ? instructor.getNombreCompleto() : "Instructor";
        String diaStr = (fecha != null && !fecha.isBlank()) ? fecha : LocalDate.now().toString();
        String ficha = aprendiz.getFichaFormacion();

        NotificacionDTO dto = new NotificacionDTO("inasistencia", "⚠️ Inasistencia registrada",
                "Se registró tu inasistencia el " + diaStr + ". Instructor: " + nombreInstructor,
                nombreInstructor, ficha);
        dto.setSonar(true);
        notificationWebSocketHandler.notificarAprendicesDeFicha(ficha, dto);

        resp.put("ok", true);
        return ResponseEntity.ok(resp);
    }

    // ---------------------------------------------------------------
    // Historial del aprendiz
    // ---------------------------------------------------------------
    @GetMapping("/mi-historial")
    @Transactional
    public ResponseEntity<java.util.List<Map<String, Object>>> miHistorial(HttpSession session) {
        Aprendiz aprendiz = getAprendizFromSession(session);
        if (aprendiz == null) return ResponseEntity.status(401).build();

        java.util.List<Map<String, Object>> result = asistenciaRepository
                .findByAprendiz(aprendiz.getIdAprendiz())
                .stream().map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("fecha", r.getFecha().toString());
                    m.put("estado", r.getEstado());
                    m.put("observacion", r.getObservacion());
                    m.put("instructor", r.getInstructor() != null ? r.getInstructor().getNombreCompleto() : "");
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------
    // Reporte PDF del aprendiz
    // ---------------------------------------------------------------
    @GetMapping("/reporte/aprendiz/pdf")
    @Transactional
    public void reporteAprendizPdf(HttpSession session, HttpServletResponse response) throws IOException {
        Aprendiz aprendiz = getAprendizFromSession(session);
        if (aprendiz == null) { response.sendError(401); return; }

        java.util.List<RegistroAsistencia> registros = asistenciaRepository.findByAprendiz(aprendiz.getIdAprendiz());
        String nombre = aprendiz.getUsuario() != null
                ? (nvl(aprendiz.getUsuario().getNombres()) + " " + nvl(aprendiz.getUsuario().getApellidos())).trim()
                : "Aprendiz";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"asistencia_" + nombre.replace(" ", "_") + ".pdf\"");

        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        escribirReportePdf(doc, "Reporte de Asistencia\n" + nombre, registros, false);
        doc.close();
    }

    // ---------------------------------------------------------------
    // Reporte PDF semanal del instructor
    // ---------------------------------------------------------------
    @GetMapping("/reporte/instructor/pdf")
    @Transactional
    public void reporteInstructorPdf(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        Integer idInstructor = getIdInstructor(session);
        if (idInstructor == null) { response.sendError(401); return; }

        LocalDate hoy = LocalDate.now();
        LocalDate ini = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde) : hoy.with(DayOfWeek.MONDAY);
        LocalDate fin = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta) : hoy.with(DayOfWeek.SUNDAY);

        java.util.List<RegistroAsistencia> registros = asistenciaRepository
                .findByInstructorAndRango(idInstructor, ini, fin);

        Instructor instructor = instructorRepository.findById(idInstructor).orElse(null);
        String nombreInstructor = instructor != null ? instructor.getNombreCompleto() : "Instructor";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"asistencia_semanal_" + ini + "_" + fin + ".pdf\"");

        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        escribirReportePdf(doc,
                "Reporte Semanal de Asistencia\n" + nombreInstructor + " | " + ini + " al " + fin,
                registros, true);
        doc.close();
    }

    // ---------------------------------------------------------------
    // Helper PDF
    // ---------------------------------------------------------------
    private void escribirReportePdf(Document doc, String titulo,
                                    java.util.List<RegistroAsistencia> registros,
                                    boolean mostrarAprendiz) throws DocumentException {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Font fTitulo = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0, 107, 45));
        Font fHeader = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font fCell   = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);

        Paragraph p = new Paragraph(titulo, fTitulo);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(14);
        doc.add(p);

        Paragraph gen = new Paragraph("Generado: " + LocalDate.now().format(fmt),
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
        gen.setAlignment(Element.ALIGN_RIGHT);
        gen.setSpacingAfter(10);
        doc.add(gen);

        if (registros.isEmpty()) {
            doc.add(new Paragraph("No hay registros para el período seleccionado.",
                    new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY)));
            return;
        }

        int cols = mostrarAprendiz ? 4 : 3;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        Color verde = new Color(0, 107, 45);

        if (mostrarAprendiz) addHeaderCell(table, "Aprendiz", fHeader, verde);
        addHeaderCell(table, "Fecha", fHeader, verde);
        addHeaderCell(table, "Estado", fHeader, verde);
        addHeaderCell(table, "Observación", fHeader, verde);

        boolean alt = false;
        for (RegistroAsistencia r : registros) {
            Color bg = alt ? new Color(240, 255, 240) : Color.WHITE;
            if (mostrarAprendiz) {
                String ap = r.getAprendiz() != null && r.getAprendiz().getUsuario() != null
                        ? (nvl(r.getAprendiz().getUsuario().getNombres()) + " "
                           + nvl(r.getAprendiz().getUsuario().getApellidos())).trim() : "";
                addCell(table, ap, fCell, bg);
            }
            addCell(table, r.getFecha().format(fmt), fCell, bg);

            Color ec = "PRESENTE".equals(r.getEstado()) ? new Color(0, 107, 45)
                     : "RETRASO".equals(r.getEstado())  ? new Color(200, 100, 0)
                     : "INCAPACIDAD".equals(r.getEstado()) ? new Color(111, 66, 193)
                     : new Color(180, 0, 0);
            PdfPCell ce = new PdfPCell(new Phrase(nvl(r.getEstado()),
                    new Font(Font.HELVETICA, 9, Font.BOLD, ec)));
            ce.setBackgroundColor(bg);
            ce.setPadding(6);
            table.addCell(ce);

            addCell(table, nvl(r.getObservacion()), fCell, bg);
            alt = !alt;
        }
        doc.add(table);

        long presentes = registros.stream().filter(r -> "PRESENTE".equals(r.getEstado())).count();
        long retrasos  = registros.stream().filter(r -> "RETRASO".equals(r.getEstado())).count();
        long ausentes  = registros.stream().filter(r -> "AUSENTE".equals(r.getEstado())).count();

        Paragraph res = new Paragraph(
                "\nResumen — Presentes: " + presentes + "   Retrasos: " + retrasos + "   Ausentes: " + ausentes,
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK));
        res.setSpacingBefore(12);
        doc.add(res);
    }

    private void addHeaderCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(6);
        t.addCell(c);
    }
}
