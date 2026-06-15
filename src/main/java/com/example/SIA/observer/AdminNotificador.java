package com.example.SIA.observer;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.service.NotificacionService;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminNotificador implements ObservadorEvento {

    private final NotificacionService notificacionService;
    private final NotificationWebSocketHandler wsHandler;

    public AdminNotificador(NotificacionService notificacionService,
                            NotificationWebSocketHandler wsHandler) {
        this.notificacionService = notificacionService;
        this.wsHandler = wsHandler;
        SistemaEventos.registrarObservador(this);
    }

    @Override
    public void notificar(Evento evento) {

        if (evento instanceof EventoUsuarioRegistrado e) {
            String msg = "Nuevo usuario registrado: " + e.getUsuario().getNombres()
                       + " " + e.getUsuario().getApellidos();
            notificacionService.crear(msg, "usuario_registro", "usuarios", "media");

            wsHandler.notificarAdmin(new NotificacionDTO(
                    "usuario_registro",
                    "👤 Nuevo usuario registrado",
                    msg,
                    "Sistema",
                    null
            ));
        }

        if (evento instanceof EventoCertificadoDescargado e) {
            String msg = "Instructor " + e.getInstructor().getNombres()
                       + " descargó certificado: " + e.getTipoCertificado();
            notificacionService.crear(msg, "certificado_descargado", "instructores", "media");

            wsHandler.notificarAdmin(new NotificacionDTO(
                    "certificado",
                    "📄 Certificado descargado",
                    msg,
                    e.getInstructor().getNombres(),
                    null
            ));
        }
    }
}
