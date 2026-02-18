package com.example.SIA.websocket;

import com.example.SIA.dto.NotificacionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket Handler para notificaciones en tiempo real
 * Conecta instructores y aprendices por ficha para recibir notificaciones de nuevos mensajes
 */
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Map: ficha -> lista de sesiones de aprendices
    private final Map<String, List<WebSocketSession>> aprendicesPorFicha = new ConcurrentHashMap<>();
    
    // Map: instructorId -> lista de sesiones de instructores
    private final Map<String, List<WebSocketSession>> instructoresPorId = new ConcurrentHashMap<>();
    
    // Map: sesion -> tipo de usuario (aprendiz o instructor)
    private final Map<WebSocketSession, String> tipoUsuario = new ConcurrentHashMap<>();
    
    // Map: sesion -> id de usuario (ficha para aprendices, instructorId para instructores)
    private final Map<WebSocketSession, String> usuarioId = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        
        // Formato: /notificaciones/{tipo}/{id}
        // Tipo: "aprendiz" o "instructor"
        // id: ficha para aprendices, instructorId para instructores
        
        if (parts.length >= 4) {
            String tipo = parts[parts.length - 2];
            String id = parts[parts.length - 1];
            
            tipoUsuario.put(session, tipo);
            usuarioId.put(session, id);
            
            if ("aprendiz".equalsIgnoreCase(tipo)) {
                aprendicesPorFicha.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(session);
                logger.info("🔔 Aprendiz conectado a notificaciones - Ficha: {}, Session: {}", id, session.getId());
            } else if ("instructor".equalsIgnoreCase(tipo)) {
                instructoresPorId.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(session);
                logger.info("🔔 Instructor conectado a notificaciones - ID: {}, Session: {}", id, session.getId());
            }
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Por ahora no procesamos mensajes entrantes, solo enviamos notificaciones
        logger.debug("Mensaje recibido en notificaciones (ignorado): {}", message.getPayload());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String tipo = tipoUsuario.remove(session);
        String id = usuarioId.remove(session);
        
        if ("aprendiz".equalsIgnoreCase(tipo)) {
            List<WebSocketSession> sesiones = aprendicesPorFicha.get(id);
            if (sesiones != null) {
                sesiones.remove(session);
                if (sesiones.isEmpty()) {
                    aprendicesPorFicha.remove(id);
                }
            }
            logger.info("🔔 Aprendiz desconectado - Ficha: {}", id);
        } else if ("instructor".equalsIgnoreCase(tipo)) {
            List<WebSocketSession> sesiones = instructoresPorId.get(id);
            if (sesiones != null) {
                sesiones.remove(session);
                if (sesiones.isEmpty()) {
                    instructoresPorId.remove(id);
                }
            }
            logger.info("🔔 Instructor desconectado - ID: {}", id);
        }
    }
    
    /**
     * Notificar a todos los aprendices de una ficha sobre un nuevo mensaje
     */
    public void notificarAprendicesDeFicha(String ficha, NotificacionDTO notificacion) {
        List<WebSocketSession> sesiones = aprendicesPorFicha.get(ficha);
        if (sesiones != null && !sesiones.isEmpty()) {
            enviarNotificacion(sesiones, notificacion);
            logger.info("🔔 Notificación enviada a {} aprendices de ficha {}", sesiones.size(), ficha);
        }
    }
    
    /**
     * Notificar a un instructor específico
     */
    public void notificarInstructor(String instructorId, NotificacionDTO notificacion) {
        List<WebSocketSession> sesiones = instructoresPorId.get(instructorId);
        if (sesiones != null && !sesiones.isEmpty()) {
            enviarNotificacion(sesiones, notificacion);
            logger.info("🔔 Notificación enviada a instructor {}", instructorId);
        }
    }
    
    /**
     * Notificar a todos los aprendices de múltiples fichas
     */
    public void notificarAprendicesDeMultiplesFichas(List<String> fichas, NotificacionDTO notificacion) {
        fichas.forEach(ficha -> notificarAprendicesDeFicha(ficha, notificacion));
    }
    
    /**
     * Enviar notificación a una lista de sesiones
     */
    private void enviarNotificacion(List<WebSocketSession> sesiones, NotificacionDTO notificacion) {
        try {
            String payload = mapper.writeValueAsString(notificacion);
            for (WebSocketSession session : sesiones) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (Exception e) {
                        logger.error("Error enviando notificación a sesión {}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error serializando notificación", e);
        }
    }
    
    /**
     * Obtener estadísticas de conexiones activas
     */
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("aprendices_conectados", aprendicesPorFicha.values().stream().mapToInt(List::size).sum());
        stats.put("instructores_conectados", instructoresPorId.values().stream().mapToInt(List::size).sum());
        stats.put("fichas_activas", aprendicesPorFicha.size());
        stats.put("instructores_activos", instructoresPorId.size());
        return stats;
    }
}
