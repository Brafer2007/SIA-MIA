package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Token de verificación de correo para nuevos registros.
 */
@Entity
@Table(name = "token_verificacion")
public class TokenVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String correo;

    @Column(nullable = false)
    private LocalDateTime expiracion;

    private boolean usado = false;

    public TokenVerificacion() {}

    public TokenVerificacion(String token, String correo, LocalDateTime expiracion) {
        this.token = token;
        this.correo = correo;
        this.expiracion = expiracion;
    }

    public boolean isValido() {
        return !usado && LocalDateTime.now().isBefore(expiracion);
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public String getCorreo() { return correo; }
    public LocalDateTime getExpiracion() { return expiracion; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
