package com.example.SIA.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.example.SIA.websocket.ChatWebSocketHandler;
import com.example.SIA.websocket.NotificationWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Chat WebSocket: Accept connections both from apprentices (by ficha) and instructors (ficha + instructorId)
        registry.addHandler(chatHandler(), "/chat/{ficha}/{instructorId}")
            .setAllowedOrigins("*");
        registry.addHandler(chatHandler(), "/chat/{ficha}")
            .setAllowedOrigins("*");
        
        // Notification WebSocket: Para notificaciones en tiempo real
        // Formato: /notificaciones/{tipo}/{id}
        // tipo: "aprendiz" o "instructor"
        // id: ficha para aprendices, instructorId para instructores
        registry.addHandler(notificationHandler(), "/notificaciones/{tipo}/{id}")
            .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
    
    @Bean
    public NotificationWebSocketHandler notificationHandler() {
        return new NotificationWebSocketHandler();
    }
}
