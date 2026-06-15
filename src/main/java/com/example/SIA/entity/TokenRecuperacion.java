
package com.example.SIA.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "expiracion", nullable = false)
    private LocalDateTime expiracion;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    public TokenRecuperacion() {}

    public TokenRecuperacion(String token, String correo, LocalDateTime expiracion) {
        this.token = token;
        this.correo = correo;
        this.expiracion = expiracion;
        this.usado = false;
    }

    public boolean isValido() {
        return !usado && LocalDateTime.now().isBefore(expiracion);
    }

    // Getters y setters
    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public LocalDateTime getExpiracion() { return expiracion; }
    public void setExpiracion(LocalDateTime expiracion) { this.expiracion = expiracion; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
