package com.example.SIA.util;

import com.example.SIA.dto.UsuarioResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExcelExporter {

    public static byte[] exportarUsuarios(List<UsuarioResponse> usuarios) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Usuarios SENA");

            String[] headers = {"Nombre", "Correo", "Documento", "Perfil", "Estado"};

            // Encabezado
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowIdx = 1;
            for (UsuarioResponse u : usuarios) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getNombreUsuario());
                row.createCell(1).setCellValue(u.getCorreo());
                row.createCell(2).setCellValue(u.getNoDocumento());
                row.createCell(3).setCellValue(u.getNombrePerfil());
                row.createCell(4).setCellValue(u.getEstado() == 1 ? "Activo" : "Inactivo");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al exportar Excel", e);
        }
    }
}
