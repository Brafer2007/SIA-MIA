package com.example.SIA.dto;

import java.time.LocalDateTime;

public class TareaAprendizDTO {

    private Long idTarea;
    private String titulo;
    private String descripcion;
    private String nombreInstructor;
    private LocalDateTime fechaLimite;
    /** Valores posibles: "PENDIENTE", "ENTREGADA", "CALIFICADA", "VENCIDA" */
    private String estadoEntrega;
    private Double nota;
    private String comentarioInstructor;
    private boolean tieneArchivoTarea;
    private String rutaArchivoTarea;

    public Long getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Long idTarea) {
        this.idTarea = idTarea;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNombreInstructor() {
        return nombreInstructor;
    }

    public void setNombreInstructor(String nombreInstructor) {
        this.nombreInstructor = nombreInstructor;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public String getEstadoEntrega() {
        return estadoEntrega;
    }

    public void setEstadoEntrega(String estadoEntrega) {
        this.estadoEntrega = estadoEntrega;
    }

    public Double getNota() {
        return nota;
    }

    public void setNota(Double nota) {
        this.nota = nota;
    }

    public String getComentarioInstructor() {
        return comentarioInstructor;
    }

    public void setComentarioInstructor(String comentarioInstructor) {
        this.comentarioInstructor = comentarioInstructor;
    }

    public boolean isTieneArchivoTarea() {
        return tieneArchivoTarea;
    }

    public void setTieneArchivoTarea(boolean tieneArchivoTarea) {
        this.tieneArchivoTarea = tieneArchivoTarea;
    }

    public String getRutaArchivoTarea() {
        return rutaArchivoTarea;
    }

    public void setRutaArchivoTarea(String rutaArchivoTarea) {
        this.rutaArchivoTarea = rutaArchivoTarea;
    }
}
