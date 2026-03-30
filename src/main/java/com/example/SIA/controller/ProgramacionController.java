package com.example.SIA.controller;

import com.example.SIA.dto.FestivoDTO;
import com.example.SIA.dto.ProgramacionDTO;
import com.example.SIA.entity.Instructor;
import com.example.SIA.entity.Programacion;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.ProgramacionRepository;
import com.example.SIA.service.InstructorService;
import com.example.SIA.service.FestivoService;
import com.example.SIA.pattern.strategy.AjusteCalendarioStrategy; // PATRON STRATEGY

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/programacion")
public class ProgramacionController {

    private static final Logger logger = LoggerFactory.getLogger(ProgramacionController.class);

    @Autowired
    private ProgramacionRepository programacionRepository;

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private FestivoService festivoService;

    @Autowired
    private AjusteCalendarioStrategy ajusteCalendarioStrategy; // Inyección de la Estrategia

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadProgramacion(@RequestParam("file") MultipartFile file,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        List<Programacion> listaProgramacion = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<String> muestrasIgnoradas = new ArrayList<>();

        if (file.isEmpty()) {
            response.put("error", "El archivo está vacío ❌");
            return ResponseEntity.badRequest().body(response);
        }

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Instructor instructorLogueado = usuario.getInstructor();
            String nombreInstructorSesion = normalizar(instructorLogueado.getNombreCompleto());

            int totalFilas = 0;
            int filasProcesadas = 0;
            int filasIgnoradas = 0;

            // 🔁 Propagar mes hacia la derecha
            Map<Integer, String> mapaMesesPorColumna = new HashMap<>();
            String mesActual = "";
            Row filaMeses = sheet.getRow(0);
            for (int col = 9; col < filaMeses.getLastCellNum(); col++) {
                Cell cell = filaMeses.getCell(col);
                if (cell != null) {
                    String posibleMes = getCellValue(cell).trim().toUpperCase();
                    if (!posibleMes.isEmpty())
                        mesActual = posibleMes;
                }
                mapaMesesPorColumna.put(col, mesActual);
            }

            for (Row row : sheet) {
                if (row.getRowNum() <= 2)
                    continue;
                totalFilas++;

                String aula = getCellValue(row.getCell(0));
                String ficha = getCellValue(row.getCell(2));
                String trimestre = getCellValue(row.getCell(3));
                String programa = getCellValue(row.getCell(4));
                logger.info("Fila " + row.getRowNum() + " - Programa leído: '" + programa + "'");
                String competencia = getCellValue(row.getCell(6));
                String instructorNombreExcel = getCellValue(row.getCell(7));
                String hora = getCellValue(row.getCell(8));

                if (instructorNombreExcel == null || instructorNombreExcel.isEmpty()) {
                    filasIgnoradas++;
                    if (muestrasIgnoradas.size() < 10) muestrasIgnoradas.add("Fila " + row.getRowNum() + ": instructor vacío");
                    continue;
                }

                if (!normalizar(instructorNombreExcel).equals(nombreInstructorSesion)) {
                    filasIgnoradas++;
                    if (muestrasIgnoradas.size() < 10) muestrasIgnoradas.add("Fila " + row.getRowNum() + ": '" + instructorNombreExcel + "'");
                    continue;
                }

                Instructor instructor = instructorLogueado;
                String[] horas = hora.split("A");
                String horaInicio = horas.length > 0 ? horas[0].trim() : "";
                String horaFin = horas.length > 1 ? horas[1].trim() : "";

                for (int col = 9; col < row.getLastCellNum(); col++) {
                    Cell cellData = row.getCell(col);
                    if (cellData == null)
                        continue;

                    String valorDia = getCellValue(cellData).trim().toUpperCase();
                    if (!valorDia.contains("P"))
                        continue;

                    String diaNumero = getCellValue(sheet.getRow(2).getCell(col)).trim();
                    String mes = mapaMesesPorColumna.getOrDefault(col, "").toUpperCase();

                    if (diaNumero.matches("\\d+") && !mes.isEmpty()) {
                        String dia = diaNumero.replaceFirst("^0+", "") + " " + mes;

                        Programacion p = new Programacion();
                        p.setAula(aula);
                        p.setCurso(!competencia.isEmpty() ? competencia : programa);
                        p.setDia(dia);
                        p.setHoraInicio(horaInicio);
                        p.setHoraFin(horaFin);
                        p.setNombreFicha(ficha);
                        p.setTrimestre(trimestre);
                        p.setInstructor(instructor);
                        p.setPrograma(programa);

                        listaProgramacion.add(p);
                        filasProcesadas++;
                    }
                }
            }

            // ==========================================================================================
            // PATRÓN STRATEGY: Delegamos el algoritmo de ajuste de fechas
            // ==========================================================================================
            try {
                // 1. Obtenemos los festivos (Contexto)
                List<FestivoDTO> festivosDTO = festivoService.obtenerFestivosDeColombia();

                // 2. Ejecutamos la estrategia configurada
                // Esto reemplaza toda la lógica compleja de "while(esDiaNoHabil)..."
                ajusteCalendarioStrategy.ajustar(listaProgramacion, festivosDTO);

                logger.info("Estrategia de ajuste aplicada correctamente.");

            } catch (Exception e) {
                logger.error("Error aplicando estrategia de festivos: ", e);
                errores.add("Advertencia: No se pudieron calcular los festivos. Se guardaron fechas originales.");
            }
            // ==========================================================================================

            programacionRepository.saveAll(listaProgramacion);

            response.put("mensaje", "Archivo cargado y procesado corectamente. "
                    + (errores.isEmpty() ? "Se ajustaron fechas por festivos ✅" : "Sin ajuste de festivos."));
            response.put("totalFilas", totalFilas);
            response.put("filasProcesadas", filasProcesadas);
            response.put("filasIgnoradas", filasIgnoradas);
            response.put("programacion",
                    listaProgramacion.stream().map(ProgramacionDTO::new).collect(Collectors.toList()));
                response.put("muestrasIgnoradas", muestrasIgnoradas);
            return ResponseEntity.ok(response);

        } catch (Throwable e) {
            e.printStackTrace();
            String errorMsg = "Error inesperado (" + e.getClass().getSimpleName() + "): " + e.getMessage();
            logger.error(errorMsg, e);
            response.put("error", errorMsg);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/upload-admin")
    public ResponseEntity<Map<String, Object>> uploadProgramacionAdmin(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        List<Programacion> listaProgramacion = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<String> muestrasIgnoradas = new ArrayList<>();

        if (file.isEmpty()) {
            response.put("error", "El archivo está vacío ❌");
            return ResponseEntity.badRequest().body(response);
        }

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int totalFilas = 0;
            int filasProcesadas = 0;
            int filasIgnoradas = 0;

            // 🔁 Propagar mes hacia la derecha
            Map<Integer, String> mapaMesesPorColumna = new HashMap<>();
            String mesActual = "";
            Row filaMeses = sheet.getRow(0);
            for (int col = 9; col < filaMeses.getLastCellNum(); col++) {
                Cell cell = filaMeses.getCell(col);
                if (cell != null) {
                    String posibleMes = getCellValue(cell).trim().toUpperCase();
                    if (!posibleMes.isEmpty())
                        mesActual = posibleMes;
                }
                mapaMesesPorColumna.put(col, mesActual);
            }

            for (Row row : sheet) {
                if (row.getRowNum() <= 2)
                    continue;
                totalFilas++;

                String aula = getCellValue(row.getCell(0));
                String ficha = getCellValue(row.getCell(2));
                String trimestre = getCellValue(row.getCell(3));
                String programa = getCellValue(row.getCell(4));
                logger.info("Fila " + row.getRowNum() + " - Programa leído: '" + programa + "'");
                String competencia = getCellValue(row.getCell(6));
                String instructorNombreExcel = getCellValue(row.getCell(7));
                String hora = getCellValue(row.getCell(8));

                if (instructorNombreExcel == null || instructorNombreExcel.isEmpty()) {
                    filasIgnoradas++;
                    if (muestrasIgnoradas.size() < 10) muestrasIgnoradas.add("Fila " + row.getRowNum() + ": instructor vacío");
                    continue;
                }

                // Buscar instructor por nombre
                Instructor instructor = null;
                try {
                    instructor = instructorService.buscarPorNombreNormalizado(instructorNombreExcel);
                } catch (Exception e) {
                    filasIgnoradas++;
                    if (muestrasIgnoradas.size() < 10) muestrasIgnoradas.add("Fila " + row.getRowNum() + ": instructor '" + instructorNombreExcel + "' no encontrado");
                    continue;
                }

                String[] horas = hora.split("A");
                String horaInicio = horas.length > 0 ? horas[0].trim() : "";
                String horaFin = horas.length > 1 ? horas[1].trim() : "";

                for (int col = 9; col < row.getLastCellNum(); col++) {
                    Cell cellData = row.getCell(col);
                    if (cellData == null)
                        continue;

                    String valorDia = getCellValue(cellData).trim().toUpperCase();
                    if (!valorDia.contains("P"))
                        continue;

                    String diaNumero = getCellValue(sheet.getRow(2).getCell(col)).trim();
                    String mes = mapaMesesPorColumna.getOrDefault(col, "").toUpperCase();

                    if (diaNumero.matches("\\d+") && !mes.isEmpty()) {
                        String dia = diaNumero.replaceFirst("^0+", "") + " " + mes;

                        Programacion p = new Programacion();
                        p.setAula(aula);
                        p.setCurso(!competencia.isEmpty() ? competencia : programa);
                        p.setDia(dia);
                        p.setHoraInicio(horaInicio);
                        p.setHoraFin(horaFin);
                        p.setNombreFicha(ficha);
                        p.setTrimestre(trimestre);
                        p.setInstructor(instructor);
                        p.setPrograma(programa);

                        listaProgramacion.add(p);
                        filasProcesadas++;
                    }
                }
            }

            // ⚠️ NO aplicar ajuste de festivos en admin upload
            // (Para evitar que mueva fechas a meses adicionales)
            // Si se necesita, descomentar:
            /*
            try {
                List<FestivoDTO> festivosDTO = festivoService.obtenerFestivosDeColombia();
                ajusteCalendarioStrategy.ajustar(listaProgramacion, festivosDTO);
                logger.info("Estrategia de ajuste aplicada correctamente.");
            } catch (Exception e) {
                logger.error("Error aplicando estrategia de festivos: ", e);
                errores.add("Advertencia: No se pudieron calcular los festivos. Se guardaron fechas originales.");
            }
            */

            // Guardar sin eliminar anteriores (agregar nuevos datos)
            programacionRepository.saveAll(listaProgramacion);

            response.put("mensaje", "Archivo cargado y procesado correctamente (Admin). "
                    + (errores.isEmpty() ? "Se ajustaron fechas por festivos ✅" : "Sin ajuste de festivos."));
            response.put("totalFilas", totalFilas);
            response.put("filasProcesadas", filasProcesadas);
            response.put("filasIgnoradas", filasIgnoradas);
            response.put("programacion",
                    listaProgramacion.stream().map(ProgramacionDTO::new).collect(Collectors.toList()));
            response.put("muestrasIgnoradas", muestrasIgnoradas);
            return ResponseEntity.ok(response);

        } catch (Throwable e) {
            e.printStackTrace();
            String errorMsg = "Error inesperado (" + e.getClass().getSimpleName() + "): " + e.getMessage();
            logger.error(errorMsg, e);
            response.put("error", errorMsg);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/instructor/{id}")
    public ResponseEntity<List<ProgramacionDTO>> obtenerProgramacionPorInstructor(@PathVariable Long id) {
        List<ProgramacionDTO> lista = programacionRepository.findByInstructorId(id)
                .stream()
                .map(ProgramacionDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/fichas/{idInstructor}")
    public ResponseEntity<List<String>> obtenerFichasUnicas(@PathVariable Long idInstructor) {
        List<String> fichas = programacionRepository.findByInstructorId(idInstructor)
                .stream()
                .map(Programacion::getNombreFicha)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(fichas);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarProgramacion(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();
        try {
            programacionRepository.deleteById(id);
            response.put("mensaje", "Programación eliminada correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al eliminar: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @DeleteMapping("/admin/todos")
    public ResponseEntity<Map<String, Object>> eliminarTodasLasProgramaciones() {
        Map<String, Object> response = new HashMap<>();
        try {
            long cantidad = programacionRepository.count();
            programacionRepository.deleteAll();
            response.put("mensaje", "Se eliminaron " + cantidad + " programaciones correctamente");
            response.put("cantidad", cantidad);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Error al eliminar: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @GetMapping("/todos-instructores")
    public ResponseEntity<List<Map<String, Object>>> obtenerTodosLosInstructoresConProgramacion() {
        List<Instructor> instructores = instructorService.obtenerTodos();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Instructor instructor : instructores) {
            Map<String, Object> instructorData = new HashMap<>();
            instructorData.put("id", instructor.getId());
            instructorData.put("nombre", instructor.getNombreCompleto());
            instructorData.put("programacion", 
                programacionRepository.findByInstructorId((long)instructor.getId())
                    .stream()
                    .map(ProgramacionDTO::new)
                    .collect(Collectors.toList())
            );
            resultado.add(instructorData);
        }

        return ResponseEntity.ok(resultado);
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        yield String.valueOf((long) value);
                    } else {
                        yield String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private String normalizar(String nombre) {
        if (nombre == null)
            return "";
        return nombre.trim()
                .toUpperCase()
                .replaceAll("[ÁÀÂÄ]", "A")
                .replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I")
                .replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U")
                .replaceAll("\\s+", " ");
    }

}
