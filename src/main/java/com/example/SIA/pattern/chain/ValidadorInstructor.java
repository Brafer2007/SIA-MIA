package com.example.SIA.pattern.chain;

import com.example.SIA.entity.Instructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.text.Normalizer;

public class ValidadorInstructor extends ValidadorFila {

    @Override
    public boolean validar(Row fila, Instructor instructorSesion) {
        Cell cellNombre = fila.getCell(7); // Columna 7 es Intentuctor
        String instructorNombreExcel = "";

        if (cellNombre != null) {
            instructorNombreExcel = cellNombre.toString();
        }

        if (instructorNombreExcel == null || instructorNombreExcel.trim().isEmpty()) {
            return false; // Ignorar si no hay instructor
        }

        String nombreNormalizadoExcel = normalizar(instructorNombreExcel);
        String nombreNormalizadoSesion = normalizar(instructorSesion.getNombreCompleto());

        if (!nombreNormalizadoExcel.equals(nombreNormalizadoSesion)) {
            return false; // Ignorar si no es el instructor logueado
        }

        return checkSiguiente(fila, instructorSesion);
    }

    private String normalizar(String nombre) {
        if (nombre == null)
            return "";
        return nombre.trim().toUpperCase()
                .replaceAll("[ÁÀÂÄ]", "A")
                .replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I")
                .replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U")
                .replaceAll("\\s+", " ");
    }
}
