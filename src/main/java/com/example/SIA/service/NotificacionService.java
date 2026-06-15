package com.example.SIA.service;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.entity.Notificacion;
import com.example.SIA.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;

    // ── Admin: notificaciones de sistema (destinatarioId IS NULL) ────────────

    /** Crea notificación de sistema para el admin. */
    public void crear(String mensaje, String tipo, String categoria, String prioridad) {
        Notificacion n = new Notificacion();
        n.setTitulo(tipo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setCategoria(categoria);
        n.setPrioridad(prioridad);
        repo.save(n);
    }

    /** Notificaciones no leídas del admin (sistema). */
    public List<Notificacion> obtenerNoLeidas() {
        return repo.findByLeidaFalseAndDestinatarioIdIsNull();
    }

    /** Marca como leídas las notificaciones de sistema del admin. */
    public void marcarComoLeidas() {
        List<Notificacion> lista = repo.findByLeidaFalseAndDestinatarioIdIsNull();
        lista.forEach(n -> n.setLeida(true));
        repo.saveAll(lista);
    }

    /**
     * Historial del admin: últimas 50 notificaciones de sistema,
     * filtradas solo por tipos relevantes al admin.
     */
    public List<NotificacionDTO> obtenerHistorialAdmin() {
        return repo.findTop50ByDestinatarioIdIsNullOrderByFechaDesc()
                .stream()
                .filter(n -> isAdminTipo(n.getTipo()))
                .map(n -> toDTO(n, n.isLeida()))
                .collect(Collectors.toList());
    }

    /** Notificaciones de sistema no leídas para el admin. */
    public List<NotificacionDTO> obtenerPendientesAdmin() {
        return repo.findByLeidaFalseAndDestinatarioIdIsNull()
                .stream()
                .filter(n -> isAdminTipo(n.getTipo()))
                .map(n -> toDTO(n, false))
                .collect(Collectors.toList());
    }

    // ── Usuarios específicos (aprendiz/instructor) ───────────────────────────

    /** Persiste notificación para un usuario específico. */
    public void crearParaUsuario(Integer idUsuario, String rol,
                                  String titulo, String mensaje, String tipo) {
        Notificacion n = new Notificacion();
        n.setDestinatarioId(idUsuario);
        n.setDestinatarioRol(rol);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setCategoria("general");
        n.setPrioridad("media");
        repo.save(n);
    }

    /** Notificaciones no leídas de un usuario. */
    public List<NotificacionDTO> obtenerPendientesParaUsuario(Integer idUsuario) {
        return repo.findByDestinatarioIdAndLeidaFalseOrderByFechaDesc(idUsuario)
                .stream()
                .map(n -> toDTO(n, false))
                .collect(Collectors.toList());
    }

    /** Últimas 50 notificaciones de un usuario (leídas y no leídas). */
    public List<NotificacionDTO> obtenerHistorialParaUsuario(Integer idUsuario) {
        return repo.findTop50ByDestinatarioIdOrderByFechaDesc(idUsuario)
                .stream()
                .map(n -> toDTO(n, n.isLeida()))
                .collect(Collectors.toList());
    }

    /** Marca como leídas todas las notificaciones de un usuario. */
    public void marcarLeidasPorUsuario(Integer idUsuario) {
        repo.marcarLeidasPorDestinatario(idUsuario);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Solo estos tipos le llegan al admin. */
    private boolean isAdminTipo(String tipo) {
        if (tipo == null) return true;
        return tipo.equals("usuario_registro")
            || tipo.equals("certificado")
            || tipo.equals("certificado_descargado")
            || tipo.equals("acceso")
            || tipo.equals("error")
            || tipo.equals("general");
    }

    private NotificacionDTO toDTO(Notificacion n, boolean leida) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setTipo(n.getTipo());
        dto.setTitulo(n.getTitulo() != null ? n.getTitulo() : n.getTipo());
        dto.setMensaje(n.getMensaje());
        dto.setSonar(false);
        dto.setRolRemitente(leida ? "leida" : "no_leida");
        return dto;
    }
}
