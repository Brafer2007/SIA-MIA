package com.example.SIA.websocket;

import com.example.SIA.dto.NotificacionDTO;
import org.springframework.stereotype.Component;

/**
 * Manager centralizado para manejar las notificaciones
 * Proporciona una referencia global al NotificationWebSocketHandler
 */
@Component
public class NotificationManager {
    
    private static NotificationWebSocketHandler handler;
    
    public NotificationManager(NotificationWebSocketHandler notificationHandler) {
        NotificationManager.handler = notificationHandler;
    }
    
    /**
     * Obtener la instancia del handler
     */
    public static NotificationWebSocketHandler getHandler() {
        return handler;
    }
    
    /**
     * Notificar a aprendices de una ficha
     */
    public static void notificarAprendicesDeFicha(String ficha, NotificacionDTO notificacion) {
        if (handler != null) {
            handler.notificarAprendicesDeFicha(ficha, notificacion);
        }
    }
    
    /**
     * Notificar a un instructor
     */
    public static void notificarInstructor(String instructorId, NotificacionDTO notificacion) {
        if (handler != null) {
            handler.notificarInstructor(instructorId, notificacion);
        }
    }
    
    /**
     * Notificar a aprendices de múltiples fichas
     */
    public static void notificarAprendicesDeMultiplesFichas(java.util.List<String> fichas, NotificacionDTO notificacion) {
        if (handler != null) {
            handler.notificarAprendicesDeMultiplesFichas(fichas, notificacion);
        }
    }
}
