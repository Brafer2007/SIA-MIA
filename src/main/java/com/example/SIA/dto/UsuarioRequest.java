package com.example.SIA.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // 👈 genera getters, setters, toString automáticamente
public class UsuarioRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nombreUsuario;

    private String nombres;   // Nombre(s) reales de la persona
    private String apellidos; // Apellidos

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ingresar un correo válido")
    private String correo;

    @NotBlank(message = "El número de documento es obligatorio")
    private String documento;

    @NotBlank(message = "La clave es obligatoria")
    private String clave;

    private Integer perfil; // ID del perfil (1=Aprendiz, 2=Admin, etc.)
}
