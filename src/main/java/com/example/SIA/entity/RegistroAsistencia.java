package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_asistencia",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_aprendiz", "fecha", "id_instructor"}))
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aprendiz")
    private Aprendiz aprendiz;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_instructor")
    private Instructor instructor;

    @Column(nullable = false)
    private LocalDate fecha;

    /** PRESENTE, RETRASO, AUSENTE, INCAPACIDAD */
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
