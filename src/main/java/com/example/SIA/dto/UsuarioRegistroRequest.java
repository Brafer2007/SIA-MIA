package com.example.SIA.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UsuarioRegistroRequest {

    @NotBlank
    @Size(min = 3, message = "El nombre de usuario debe tener al menos 3 caracteres")
    private String nombreUsuario;

    @NotBlank
    @Size(min = 2, message = "Los nombres deben tener al menos 2 caracteres")
    private String nombres;

    @NotBlank
    @Size(min = 2, message = "Los apellidos deben tener al menos 2 caracteres")
    private String apellidos;

    @NotBlank
    @Email(message = "El correo no es válido")
    private String correo;

    @NotBlank
    private String noDocumento;

    @NotBlank
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String passUsuario;

    @NotBlank
    private String confirmPassword;

    // ✅ Getters y Setters
    // (puedes generarlos con Lombok si quieres)
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNoDocumento() { return noDocumento; }
    public void setNoDocumento(String noDocumento) { this.noDocumento = noDocumento; }

    public String getPassUsuario() { return passUsuario; }
    public void setPassUsuario(String passUsuario) { this.passUsuario = passUsuario; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
