package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaLimite;

    @Column(nullable = false)
    private String nombreFicha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_instructor")
    private Instructor instructor;

    private String rutaArchivo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** True si ya se envió recordatorio de vencimiento próximo */
    @Column(name = "recordatorio_enviado", nullable = false)
    private boolean recordatorioEnviado = false;

    public Tarea() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDateTime fechaLimite) { this.fechaLimite = fechaLimite; }

    public String getNombreFicha() { return nombreFicha; }
    public void setNombreFicha(String nombreFicha) { this.nombreFicha = nombreFicha; }

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isRecordatorioEnviado() { return recordatorioEnviado; }
    public void setRecordatorioEnviado(boolean recordatorioEnviado) { this.recordatorioEnviado = recordatorioEnviado; }
}
