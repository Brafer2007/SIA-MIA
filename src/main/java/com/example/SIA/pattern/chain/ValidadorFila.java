package com.example.SIA.pattern.chain;

import com.example.SIA.entity.Instructor;
import org.apache.poi.ss.usermodel.Row;

/**
 * CHAIN OF RESPONSIBILITY
 * Define una interfaz para manejar peticiones (validar filas).
 * Permite encadenar validadores sin que el cliente conozca la estructura de la
 * cadena.
 */
public abstract class ValidadorFila {

    protected ValidadorFila siguiente;

    public void setSiguiente(ValidadorFila siguiente) {
        this.siguiente = siguiente;
    }

    /**
     * Valida la fila.
     * 
     * @return true si la fila es válida y debe procesarse, false si debe ignorarse.
     */
    public abstract boolean validar(Row fila, Instructor instructorSesion);

    protected boolean checkSiguiente(Row fila, Instructor instructorSesion) {
        if (siguiente == null) {
            return true;
        }
        return siguiente.validar(fila, instructorSesion);
    }

    // Helper para obtener valores de celda de forma segura
    protected String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null)
            return "";
        /*
         * Copia simplificada del helper del controller para no duplicar código complejo
         * o delegar
         */
        return cell.toString();
    }
}
