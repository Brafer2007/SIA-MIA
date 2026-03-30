package com.example.SIA.controller;

import com.example.SIA.entity.RegistroAcceso;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.EquipoRepository;
import com.example.SIA.repository.RegistroAccesoRepository;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/admin/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final AprendizRepository aprendizRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroAccesoRepository registroAccesoRepository;

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
    // 📋 REPORTE DIARIO DE ACCESOS (JSON + Excel)
    // ==================================================================

    @GetMapping("/accesos/diario")
    @ResponseBody
    public ResponseEntity<?> reporteAccesosDiario(@RequestParam String fecha) {
        try {
            LocalDate dia = LocalDate.parse(fecha);
            LocalDateTime inicio = dia.atStartOfDay();
            LocalDateTime fin = dia.atTime(23, 59, 59);

            List<RegistroAcceso> registros = registroAccesoRepository.findByFechaHoraBetween(inicio, fin);

            long ingresos = registros.stream().filter(r -> "INGRESO".equals(r.getTipo())).count();
            long salidas  = registros.stream().filter(r -> "SALIDA".equals(r.getTipo())).count();

            List<Map<String, Object>> detalle = registros.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("hora",      r.getFechaHora().toLocalTime().toString().substring(0, 5));
                m.put("nombre",    r.getUsuario().getNombres() + " " + r.getUsuario().getApellidos());
                m.put("documento", r.getUsuario().getNoDocumento());
                m.put("tipo",      r.getTipo());
                m.put("metodo",    r.getMetodo());
                m.put("equipos",   r.getEquiposIngresados() != null ? r.getEquiposIngresados() : "");
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of(
                "fecha", fecha,
                "totalIngresos", ingresos,
                "totalSalidas", salidas,
                "detalle", detalle
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/accesos/excel")
    public ResponseEntity<byte[]> exportarAccesosExcel(@RequestParam String fecha) throws Exception {
        LocalDate dia = LocalDate.parse(fecha);
        List<RegistroAcceso> registros = registroAccesoRepository
                .findByFechaHoraBetween(dia.atStartOfDay(), dia.atTime(23, 59, 59));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Accesos " + fecha);

            // Estilo encabezado
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] cols = {"Hora", "Nombre", "Documento", "Tipo", "Método", "Equipos"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Datos
            int rowNum = 1;
            for (RegistroAcceso r : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getFechaHora().toLocalTime().toString().substring(0, 5));
                row.createCell(1).setCellValue(r.getUsuario().getNombres() + " " + r.getUsuario().getApellidos());
                row.createCell(2).setCellValue(r.getUsuario().getNoDocumento());
                row.createCell(3).setCellValue(r.getTipo());
                row.createCell(4).setCellValue(r.getMetodo());
                row.createCell(5).setCellValue(r.getEquiposIngresados() != null ? r.getEquiposIngresados() : "");
            }

            wb.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=accesos_" + fecha + ".xlsx")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        }
    }
}
