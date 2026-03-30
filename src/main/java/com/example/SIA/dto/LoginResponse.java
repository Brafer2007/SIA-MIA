package com.example.SIA.dto;

public class LoginResponse {
    private Integer idUsuario;
    private Integer idPerfil;      
    private String nombreUsuario;
    private String correo;
    private String perfil;
    private String redirect;

    public LoginResponse(Integer idUsuario, Integer idPerfil, String nombreUsuario, String correo, String perfil, String redirect) {
        this.idUsuario = idUsuario;
        this.idPerfil = idPerfil;
        this.nombreUsuario = nombreUsuario;
        this.correo = correo;
        this.perfil = perfil;
        this.redirect = redirect;
    }

    // ✅ Getters y setters
    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}