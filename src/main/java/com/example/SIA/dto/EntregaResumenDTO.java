package com.example.SIA.dto;

import java.time.LocalDateTime;

public class EntregaResumenDTO {

    private Integer idAprendiz;
    private String nombreAprendiz;
    /** Valores posibles: "ENTREGADO", "PENDIENTE" */
    private String estadoEntrega;
    private LocalDateTime fechaEntrega;
    private Long idEntrega;
    private String rutaArchivo;
    private Double nota;
    private String comentario;

    public Integer getIdAprendiz() {
        return idAprendiz;
    }

    public void setIdAprendiz(Integer idAprendiz) {
        this.idAprendiz = idAprendiz;
    }

    public String getNombreAprendiz() {
        return nombreAprendiz;
    }

    public void setNombreAprendiz(String nombreAprendiz) {
        this.nombreAprendiz = nombreAprendiz;
    }

    public String getEstadoEntrega() {
        return estadoEntrega;
    }

    public void setEstadoEntrega(String estadoEntrega) {
        this.estadoEntrega = estadoEntrega;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Long getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Long idEntrega) {
        this.idEntrega = idEntrega;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Double getNota() {
        return nota;
    }

    public void setNota(Double nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
