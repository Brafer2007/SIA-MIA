package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_acceso")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private String tipo; // "INGRESO" o "SALIDA"

    @Column(nullable = false)
    private String metodo; // "QR" o "MANUAL"

    // Guardaremos los IDs o números de serie de los equipos que ingresan como un
    // texto separado por comas
    @Column(name = "equipos_ingresados", columnDefinition = "TEXT")
    private String equiposIngresados;

    public RegistroAcceso() {
        this.fechaHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getEquiposIngresados() {
        return equiposIngresados;
    }

    public void setEquiposIngresados(String equiposIngresados) {
        this.equiposIngresados = equiposIngresados;
    }
}
