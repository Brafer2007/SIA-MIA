package com.example.SIA.controller;

import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.EquipoRepository;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.ReporteService;
import com.example.SIA.service.CertificadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final CertificadoService certificadoService;
    private final AprendizRepository aprendizRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    // ==================================================================
    // 0️⃣ DASHBOARD HTML
    // ==================================================================
    @GetMapping("")
    public String verDashboardReportes(Model model) {

        model.addAttribute("aprendicesPorPrograma", aprendizRepository.countByProgramaFormacion());
        model.addAttribute("totalEquipos", equipoRepository.countTotalEquipos());
        model.addAttribute("usuariosActivos", usuarioRepository.countActivos());
        model.addAttribute("usuariosInactivos", usuarioRepository.countInactivos());

        return "admin/reportes";
    }

    // ==================================================================
    // 🔵 ENDPOINTS JSON (para gráficas)
    // ==================================================================
    @GetMapping("/data/aprendices")
    @ResponseBody
    public Object getDataAprendices() {
        return reporteService.getAprendicesPorPrograma();
    }

    @GetMapping("/data/usuarios")
    @ResponseBody
    public Object getDataUsuarios() {
        return reporteService.getUsuariosActivosInactivos();
    }

    @GetMapping("/data/equipos")
    @ResponseBody
    public Object getDataEquipos() {
        return reporteService.getEquiposPorTipo();
    }

    // ==================================================================
    // 🔴 REPORTES PDF (inline o descarga)
    // ==================================================================

    @GetMapping("/pdf/aprendices")
    @ResponseBody
    public ResponseEntity<byte[]> verAprendices(@RequestParam(required = false) Boolean descargar) {
        byte[] pdf = reporteService.generarPdfAprendicesPorPrograma();

        String disposition = (descargar != null && descargar)
                ? "attachment; filename=aprendices_por_programa.pdf"
                : "inline; filename=aprendices_por_programa.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdf);
    }

    @GetMapping("/pdf/equipos")
    @ResponseBody
    public ResponseEntity<byte[]> verEquipos(@RequestParam(required = false) Boolean descargar) {
        byte[] pdf = reporteService.generarPdfEquipos();

        String disposition = (descargar != null && descargar)
                ? "attachment; filename=equipos_registrados.pdf"
                : "inline; filename=equipos_registrados.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdf);
    }

    @GetMapping("/pdf/usuarios")
    @ResponseBody
    public ResponseEntity<byte[]> verUsuarios(@RequestParam(required = false) Boolean descargar) {
        byte[] pdf = reporteService.generarPdfUsuariosEstado();

        String disposition = (descargar != null && descargar)
                ? "attachment; filename=usuarios_estado.pdf"
                : "inline; filename=usuarios_estado.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdf);
    }

    // ==================================================================
    // ✅ CERTIFICADOS LABORALES (Profesionales)
    // ==================================================================

    @GetMapping("/certificados/{tipo}")
    @ResponseBody
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable String tipo) {

        byte[] pdf;

        switch (tipo) {
            case "laboral":
                pdf = certificadoService.generarCertificadoLaboral();
                break;

            case "nomina":
                pdf = certificadoService.generarDesprendibleNomina();
                break;

            case "vinculacion":
                pdf = certificadoService.generarConstanciaVinculacion();
                break;

            default:
                throw new RuntimeException("Tipo de certificado no válido");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + tipo + ".pdf")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdf);
    }
}