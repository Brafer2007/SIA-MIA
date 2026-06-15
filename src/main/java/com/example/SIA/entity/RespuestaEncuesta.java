package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "respuesta_encuesta")
public class RespuestaEncuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta = LocalDateTime.now();

    // Paso 1
    @Column(name = "nombre_institucion")
    private String nombreInstitucion;

    @Column(name = "tipo_institucion")
    private String tipoInstitucion;

    @Column(name = "semestre")
    private String semestre;

    // Paso 2
    @Column(name = "asistencia_digital")
    private String asistenciaDigital;

    @Column(name = "control_acceso")
    private String controlAcceso;

    @Column(name = "problemas_asistencia")
    private Integer problemasAsistencia;

    // Paso 3
    @Column(name = "comunicacion")
    private String comunicacion;

    @Column(name = "tareas_digital")
    private String tareasDigital;

    @Column(name = "facilidad_tareas")
    private Integer facilidadTareas;

    // Paso 4
    @Column(name = "certificados")
    private String certificados;

    @Column(name = "incapacidades")
    private String incapacidades;

    @Column(name = "satisfaccion_admin")
    private Integer satisfaccionAdmin;

    // Paso 5
    @Column(name = "mayor_falencia")
    private String mayorFalencia;

    @Column(name = "usaria_sia")
    private String usariaSia;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    // Getters y Setters
    public Long getId() { return id; }
    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime v) { this.fechaRespuesta = v; }
    public String getNombreInstitucion() { return nombreInstitucion; }
    public void setNombreInstitucion(String v) { this.nombreInstitucion = v; }
    public String getTipoInstitucion() { return tipoInstitucion; }
    public void setTipoInstitucion(String v) { this.tipoInstitucion = v; }
    public String getSemestre() { return semestre; }
    public void setSemestre(String v) { this.semestre = v; }
    public String getAsistenciaDigital() { return asistenciaDigital; }
    public void setAsistenciaDigital(String v) { this.asistenciaDigital = v; }
    public String getControlAcceso() { return controlAcceso; }
    public void setControlAcceso(String v) { this.controlAcceso = v; }
    public Integer getProblemasAsistencia() { return problemasAsistencia; }
    public void setProblemasAsistencia(Integer v) { this.problemasAsistencia = v; }
    public String getComunicacion() { return comunicacion; }
    public void setComunicacion(String v) { this.comunicacion = v; }
    public String getTareasDigital() { return tareasDigital; }
    public void setTareasDigital(String v) { this.tareasDigital = v; }
    public Integer getFacilidadTareas() { return facilidadTareas; }
    public void setFacilidadTareas(Integer v) { this.facilidadTareas = v; }
    public String getCertificados() { return certificados; }
    public void setCertificados(String v) { this.certificados = v; }
    public String getIncapacidades() { return incapacidades; }
    public void setIncapacidades(String v) { this.incapacidades = v; }
    public Integer getSatisfaccionAdmin() { return satisfaccionAdmin; }
    public void setSatisfaccionAdmin(Integer v) { this.satisfaccionAdmin = v; }
    public String getMayorFalencia() { return mayorFalencia; }
    public void setMayorFalencia(String v) { this.mayorFalencia = v; }
    public String getUsariaSia() { return usariaSia; }
    public void setUsariaSia(String v) { this.usariaSia = v; }
    public String getComentario() { return comentario; }
    public void setComentario(String v) { this.comentario = v; }
}
