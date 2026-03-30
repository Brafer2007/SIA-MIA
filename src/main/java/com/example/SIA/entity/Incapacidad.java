package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incapacidad")
public class Incapacidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aprendiz")
    private Aprendiz aprendiz;

    /** Fecha de inicio de la incapacidad */
    @Column(nullable = false)
    private LocalDate fechaInicio;

    /** Fecha de fin (puede ser igual a fechaInicio para un solo día) */
    @Column(nullable = false)
    private LocalDate fechaFin;

    /** Ruta relativa del archivo adjunto (PDF/imagen) */
    private String rutaArchivo;

    /** PENDIENTE, APROBADA, RECHAZADA */
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida = LocalDateTime.now();

    /** Instructor que aprobó/rechazó */
    @ManyToOne
    @JoinColumn(name = "id_instructor_revisor")
    private Instructor instructorRevisor;

    private String observacionInstructor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public Instructor getInstructorRevisor() { return instructorRevisor; }
    public void setInstructorRevisor(Instructor instructorRevisor) { this.instructorRevisor = instructorRevisor; }

    public String getObservacionInstructor() { return observacionInstructor; }
    public void setObservacionInstructor(String obs) { this.observacionInstructor = obs; }
}
