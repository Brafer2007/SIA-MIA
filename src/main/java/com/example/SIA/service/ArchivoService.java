package com.example.SIA.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ArchivoService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "docx", "xlsx", "png", "jpg");

    /**
     * Valida que el archivo no sea nulo/vacío, tenga extensión permitida y no supere maxBytes.
     */
    public void validarArchivo(MultipartFile archivo, long maxBytes) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede ser nulo o vacío.");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new IllegalArgumentException("El archivo no tiene una extensión válida.");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Extensión no permitida: " + extension + ". Se aceptan: pdf, docx, xlsx, png, jpg.");
        }

        if (archivo.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de " + maxBytes + " bytes.");
        }
    }

    /**
     * Guarda el archivo en uploads/tareas/{idTarea}/ con nombre UUID + extensión original.
     * Retorna la ruta relativa (ej: uploads/tareas/1/uuid.pdf).
     */
    public String guardarArchivoTarea(Long idTarea, MultipartFile archivo) throws IOException {
        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;

        Path directorio = Paths.get(System.getProperty("user.dir"), "uploads", "tareas", idTarea.toString());
        Files.createDirectories(directorio);

        Path destino = directorio.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), destino);

        return "uploads/tareas/" + idTarea + "/" + nombreArchivo;
    }

    /**
     * Guarda el archivo en uploads/entregas/{idTarea}/{idAprendiz}/ con nombre UUID + extensión original.
     * Retorna la ruta relativa (ej: uploads/entregas/1/5/uuid.pdf).
     */
    public String guardarArchivoEntrega(Long idTarea, Integer idAprendiz, MultipartFile archivo) throws IOException {
        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;

        Path directorio = Paths.get(System.getProperty("user.dir"), "uploads", "entregas",
                idTarea.toString(), idAprendiz.toString());
        Files.createDirectories(directorio);

        Path destino = directorio.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), destino);

        return "uploads/entregas/" + idTarea + "/" + idAprendiz + "/" + nombreArchivo;
    }

    /**
     * Elimina el archivo del disco si existe. No lanza excepción si no existe.
     */
    public void eliminarArchivo(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return;
        }
        try {
            Path ruta = Paths.get(System.getProperty("user.dir"), rutaRelativa);
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            // No propagamos la excepción si el archivo no existe o no se puede eliminar
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String obtenerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
    }
}
