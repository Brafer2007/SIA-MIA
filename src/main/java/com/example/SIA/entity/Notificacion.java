package com.example.SIA.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotificacion;

    private String titulo;     // título corto para la campanita
    private String mensaje;

    private String tipo;       // nueva_tarea, tarea_entregada, inasistencia, nuevo_mensaje, etc.
    private String categoria;  // usuarios, instructores, equipos, general
    private String prioridad;  // baja, media, alta, critica

    /** ID del usuario destinatario (id_usuario). Null = solo admin */
    @Column(name = "destinatario_id")
    private Integer destinatarioId;

    /** Rol del destinatario: aprendiz, instructor, admin */
    @Column(name = "destinatario_rol")
    private String destinatarioRol;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha = new Date();

    private boolean leida = false;

    // ── Getters / Setters ────────────────────────────────────────
    public Integer getIdNotificacion() { return idNotificacion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public Integer getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(Integer destinatarioId) { this.destinatarioId = destinatarioId; }

    public String getDestinatarioRol() { return destinatarioRol; }
    public void setDestinatarioRol(String destinatarioRol) { this.destinatarioRol = destinatarioRol; }

    public Date getFecha() { return fecha; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}