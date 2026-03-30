package com.example.SIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//  PATRÓN FACADE 
//  PATRÓN SINGLETON
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;  
    /**
     * Envía un correo masivo a múltiples destinatarios
     * 
     * @param destinatarios Lista de emails destino
     * @param asunto Asunto del correo
     * @param mensaje Cuerpo del correo
     * @param remitente Email del remitente
     * @return true si el envío fue exitoso, false en caso contrario
     */
    public boolean enviarCorreoMasivo(List<String> destinatarios, String asunto, String mensaje, String remitente) {
        try {
            if (destinatarios == null || destinatarios.isEmpty()) {
                logger.warn("No hay destinatarios para enviar el correo");
                return false;
            }
            
            // Crear mensaje
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(remitente);
            mail.setTo(destinatarios.toArray(new String[0]));
            mail.setSubject(asunto);
            mail.setText(mensaje);
            
            // Enviar
            mailSender.send(mail);
            logger.info("Correo masivo enviado exitosamente a {} destinatarios", destinatarios.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error al enviar correo masivo: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Envía un correo individual
     * 
     * @param destinatario Email destino
     * @param asunto Asunto del correo
     * @param mensaje Cuerpo del correo
     * @param remitente Email del remitente
     * @return true si el envío fue exitoso, false en caso contrario
     */
    public boolean enviarCorreoIndividual(String destinatario, String asunto, String mensaje, String remitente) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(remitente);
            mail.setTo(destinatario);
            mail.setSubject(asunto);
            mail.setText(mensaje);
            
            mailSender.send(mail);
            logger.info("Correo enviado exitosamente a {}", destinatario);
            return true;
            
        } catch (Exception e) {
            logger.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            return false;
        }
    }
}
