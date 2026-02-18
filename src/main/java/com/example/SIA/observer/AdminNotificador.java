package com.example.SIA.observer;

import com.example.SIA.service.NotificacionService;
import org.springframework.stereotype.Component;

@Component
public class AdminNotificador implements ObservadorEvento {

    private final NotificacionService notificacionService;

    public AdminNotificador(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
        SistemaEventos.registrarObservador(this);
    }

    @Override
    public void notificar(Evento evento) {

        if (evento instanceof EventoUsuarioRegistrado e) {
            notificacionService.crear(
                    "Nuevo usuario registrado: " + e.getUsuario().getNombres(),
                    "usuario_registro",
                    "usuarios",
                    "media"
            );
        }

        if (evento instanceof EventoCertificadoDescargado e) {
            notificacionService.crear(
                    "Instructor " + e.getInstructor().getNombres() +
                    " descargó certificado: " + e.getTipoCertificado(),
                    "certificado_descargado",
                    "instructores",
                    "media"
            );
        }
    }
}