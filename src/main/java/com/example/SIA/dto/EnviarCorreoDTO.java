package com.example.SIA.dto;

import java.util.List;

public class EnviarCorreoDTO {
    
    private List<String> destinatarios; // Emails de los aprendices
    private String asunto;
    private String mensaje;
    
    public EnviarCorreoDTO() {}
    
    public EnviarCorreoDTO(List<String> destinatarios, String asunto, String mensaje) {
        this.destinatarios = destinatarios;
        this.asunto = asunto;
        this.mensaje = mensaje;
    }
    
    public List<String> getDestinatarios() {
        return destinatarios;
    }
    
    public void setDestinatarios(List<String> destinatarios) {
        this.destinatarios = destinatarios;
    }
    
    public String getAsunto() {
        return asunto;
    }
    
    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
