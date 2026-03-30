package com.example.SIA.entity;

import jakarta.persistence.*;

@Entity
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    private String especialidad;
    private String certificados;

    public Instructor() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getCertificados() {
        return certificados;
    }

    public void setCertificados(String certificados) {
        this.certificados = certificados;
    }

    // Metodo para obtener nombre completo
    public String getNombreCompleto() {
        if (usuario == null)
            return "";
        String nombres = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
        String apellidos = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
        return (nombres + " " + apellidos).toUpperCase();
    }
}