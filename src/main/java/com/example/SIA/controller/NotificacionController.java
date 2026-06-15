package com.example.SIA.controller;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API para notificaciones persistidas en BD.
 * El admin solo recibe notificaciones de sistema (usuario_registro, error, etc.)
 * Los aprendices e instructores reciben las suyas por destinatarioId.
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * GET /api/notificaciones/historial
     * - Admin (id_perfil=2): devuelve notificaciones de sistema (destinatarioId IS NULL).
     * - Otros: devuelve sus últimas 50 notificaciones personales.
     */
    @GetMapping("/historial")
    public ResponseEntity<List<NotificacionDTO>> historial(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idPerfil  = (Integer) session.getAttribute("id_perfil");
        if (idUsuario == null) return ResponseEntity.status(401).build();

        if (Integer.valueOf(2).equals(idPerfil)) {
            // Admin → solo notificaciones de sistema (sin destinatario específico)
            return ResponseEntity.ok(notificacionService.obtenerHistorialAdmin());
        }
        return ResponseEntity.ok(notificacionService.obtenerHistorialParaUsuario(idUsuario));
    }

    /**
     * GET /api/notificaciones/pendientes
     * Igual que historial pero solo las no leídas.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<NotificacionDTO>> pendientes(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idPerfil  = (Integer) session.getAttribute("id_perfil");
        if (idUsuario == null) return ResponseEntity.status(401).build();

        if (Integer.valueOf(2).equals(idPerfil)) {
            return ResponseEntity.ok(notificacionService.obtenerPendientesAdmin());
        }
        return ResponseEntity.ok(notificacionService.obtenerPendientesParaUsuario(idUsuario));
    }

    /**
     * POST /api/notificaciones/leidas
     * Marca como leídas las notificaciones del usuario en sesión.
     */
    @PostMapping("/leidas")
    public ResponseEntity<Map<String, Boolean>> marcarLeidas(HttpSession session) {
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");
        Integer idPerfil  = (Integer) session.getAttribute("id_perfil");
        if (idUsuario == null) return ResponseEntity.status(401).build();

        if (Integer.valueOf(2).equals(idPerfil)) {
            notificacionService.marcarComoLeidas(); // marca las del admin (destinatarioId null)
        } else {
            notificacionService.marcarLeidasPorUsuario(idUsuario);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
