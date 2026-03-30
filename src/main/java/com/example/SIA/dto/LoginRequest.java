package com.example.SIA.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    private String usuario;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    // ✅ Getters y setters
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}