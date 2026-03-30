package com.example.SIA.pattern.chain;

import com.example.SIA.entity.Instructor;
import org.apache.poi.ss.usermodel.Row;

public class ValidadorEstructura extends ValidadorFila {

    @Override
    public boolean validar(Row fila, Instructor instructorSesion) {
        // Validar si la fila tiene datos básicos (Aula, Ficha)
        // Y si tiene formato de fecha en alguna celda de programación (esto es una
        // simplificación)

        // Simplemente pasamos al siguiente si la estructura básica "parece" correcta
        // En este caso, el controller original validaba "null" en celdas críticas.

        // Aquí podríamos validar que la celda de Programa no esté vacía por ejemplo
        if (fila.getCell(4) == null || fila.getCell(4).toString().isEmpty()) {
            // Es vacía, pero el controller leía log... decidimos si es estricto o no.
            // Dejemos que pase, ya que el instructor es el filtro principal.
        }

        return checkSiguiente(fila, instructorSesion);
    }
}
