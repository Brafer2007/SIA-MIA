package com.example.SIA.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    private String nombreUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private String noDocumento;

    @Column(name = "pass_usuario", nullable = true)
    private String passUsuario;

    private Integer estado; // 1=activo, 0=inactivo

    /** Ruta relativa a la foto de perfil, ej: uploads/fotos/123.jpg */
    @Column(name = "foto_perfil")
    private String fotoPerfil;

    /** Intentos fallidos de login consecutivos (rate limiting) */
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    /** Si no es null, el login queda bloqueado hasta esta fecha */
    @Column(name = "bloqueado_hasta")
    private java.time.LocalDateTime bloqueadoHasta;

    // =============================
    // 🔗 GOOGLE OAUTH2
    // =============================
    @Column(name = "google_id", unique = true)
    private String googleId;

    // =============================
    // 🔥 HUELLA (BIOMETRÍA)
    // =============================
    @Lob
    @Column(name = "huella")
    private byte[] huella;

    // =============================
    // 🔗 RELACIONES
    // =============================

    @ManyToOne
    @JoinColumn(name = "id_perfil")
    private Perfil perfil;

    @JsonIgnore // 🔥 evita bucles infinitos en JSON
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Instructor instructor;

    // =============================
    // 🧠 GETTERS Y SETTERS
    // =============================

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNoDocumento() {
        return noDocumento;
    }

    public void setNoDocumento(String noDocumento) {
        this.noDocumento = noDocumento;
    }

    public String getPassUsuario() {
        return passUsuario;
    }

    public void setPassUsuario(String passUsuario) {
        this.passUsuario = passUsuario;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }

    public byte[] getHuella() {
        return huella;
    }

    public void setHuella(byte[] huella) {
        this.huella = huella;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public java.time.LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(java.time.LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}