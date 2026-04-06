package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega_tarea",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_tarea", "id_aprendiz"}))
public class EntregaTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tarea")
    private Tarea tarea;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aprendiz")
    private Aprendiz aprendiz;

    @Column(nullable = false)
    private String rutaArchivo;

    @Column(nullable = false)
    private LocalDateTime fechaEntrega;

    private Double nota;

    @Column(columnDefinition = "TEXT")
    private String comentarioInstructor;

    private LocalDateTime fechaCalificacion;

    private boolean entregaTardia = false;

    public EntregaTarea() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tarea getTarea() { return tarea; }
    public void setTarea(Tarea tarea) { this.tarea = tarea; }

    public Aprendiz getAprendiz() { return aprendiz; }
    public void setAprendiz(Aprendiz aprendiz) { this.aprendiz = aprendiz; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }

    public String getComentarioInstructor() { return comentarioInstructor; }
    public void setComentarioInstructor(String comentarioInstructor) { this.comentarioInstructor = comentarioInstructor; }

    public LocalDateTime getFechaCalificacion() { return fechaCalificacion; }
    public void setFechaCalificacion(LocalDateTime fechaCalificacion) { this.fechaCalificacion = fechaCalificacion; }

    public boolean isEntregaTardia() { return entregaTardia; }
    public void setEntregaTardia(boolean entregaTardia) { this.entregaTardia = entregaTardia; }
}
