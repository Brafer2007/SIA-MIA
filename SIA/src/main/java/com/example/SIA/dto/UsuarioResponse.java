package com.example.SIA.dto;

public class UsuarioResponse {
    private Integer idUsuario;
    private String nombreUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private String noDocumento;
    private String nombrePerfil;
    private Integer estado;

    // Constructor
    public UsuarioResponse(Integer idUsuario, String nombreUsuario, String nombres,
                           String apellidos, String correo, String noDocumento,
                           String nombrePerfil, Integer estado) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.noDocumento = noDocumento;
        this.nombrePerfil = nombrePerfil;
        this.estado = estado;
    }

    // Getters
    public Integer getIdUsuario() { return idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getNoDocumento() { return noDocumento; }
    public String getNombrePerfil() { return nombrePerfil; }
    public Integer getEstado() { return estado; }
}