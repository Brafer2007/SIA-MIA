package com.example.SIA.dto;

import java.time.LocalDateTime;

/**
 * DTO para enviar notificaciones en tiempo real a través de WebSocket
 */
public class NotificacionDTO {
    private Long idMensaje;
    private String tipo; // "nuevo_mensaje", "respuesta", "archivo"
    private String titulo;
    private String mensaje;
    private String remitente;
    private String rolRemitente;
    private String ficha;
    private String sala;
    private LocalDateTime fecha;
    private boolean sonar; // si debe reproducir sonido
    
    public NotificacionDTO() {
        this.fecha = LocalDateTime.now();
        this.sonar = true;
    }
    
    public NotificacionDTO(String tipo, String titulo, String mensaje, String remitente, String ficha) {
        this();
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.remitente = remitente;
        this.ficha = ficha;
    }
    
    // Getters y Setters
    public Long getIdMensaje() {
        return idMensaje;
    }
    
    public void setIdMensaje(Long idMensaje) {
        this.idMensaje = idMensaje;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public String getTitulo() {
        return titulo;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    public String getRemitente() {
        return remitente;
    }
    
    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }
    
    public String getRolRemitente() {
        return rolRemitente;
    }
    
    public void setRolRemitente(String rolRemitente) {
        this.rolRemitente = rolRemitente;
    }
    
    public String getFicha() {
        return ficha;
    }
    
    public void setFicha(String ficha) {
        this.ficha = ficha;
    }
    
    public String getSala() {
        return sala;
    }
    
    public void setSala(String sala) {
        this.sala = sala;
    }
    
    public LocalDateTime getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
    
    public boolean isSonar() {
        return sonar;
    }
    
    public void setSonar(boolean sonar) {
        this.sonar = sonar;
    }
}
