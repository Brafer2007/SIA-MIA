package com.example.SIA.websocket;

import com.example.SIA.entity.MensajeGrupo;
import com.example.SIA.service.MensajeService;
import com.example.SIA.dto.NotificacionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static MensajeService mensajeService;
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // Map key: instructorId -> group chat sessions (cross-ficha apprentice sessions)
    private final Map<String, List<WebSocketSession>> sesionesPorInstructor = new ConcurrentHashMap<>();
    // Map key: sala = fichaReal + "|" + instructorId (for backward compat with instructor connections)
    private final Map<String, List<WebSocketSession>> sesionesPorSala = new ConcurrentHashMap<>();
    // Map key: fichaReal -> apprentice sessions (connected by ficha only)
    private final Map<String, List<WebSocketSession>> sesionesPorFicha = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    // Track session -> salas registered (for instructors)
    private final Map<WebSocketSession, List<String>> sessionSalas = new ConcurrentHashMap<>();
    // Track apprentice sessions -> instructor ID (if in group chat mode)
    private final Map<WebSocketSession, String> sessionInstructor = new ConcurrentHashMap<>();
    // Track session -> original ficha (may be a range like "2996893 - 2996900")
    private final Map<WebSocketSession, String> sessionFichaRaw = new ConcurrentHashMap<>();

    public ChatWebSocketHandler() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @Autowired
    public void setMensajeService(MensajeService service) {
        mensajeService = service;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Path: /chat/{ficha}/{instructorId}
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        // If URL is /chat/{ficha}/{instructorId} -> register in salas
        // If URL is /chat/{ficha} -> register as apprentice in sesionesPorFicha
        if (parts.length >= 4) {
            String fichaRaw = parts[parts.length - 2];
            String instructorId = parts[parts.length - 1];

            // decode path segments in case they were encoded by client
            fichaRaw = URLDecoder.decode(fichaRaw, StandardCharsets.UTF_8);
            instructorId = URLDecoder.decode(instructorId, StandardCharsets.UTF_8);

            // Store original fichaRaw for persistence
            sessionFichaRaw.put(session, fichaRaw);

            List<String> fichasReales = obtenerFichasReales(fichaRaw);
            List<String> salas = new ArrayList<>();

            for (String f : fichasReales) {
                String sala = f + "|" + instructorId;
                salas.add(sala);
                sesionesPorSala.computeIfAbsent(sala, x -> new CopyOnWriteArrayList<>()).add(session);
            }

            sessionSalas.put(session, salas);
            logger.info("WS connect (instructor) session={} fichaRaw={} decodedFichas={} instructorId={} salas={}", session.getId(), fichaRaw, fichasReales, instructorId, salas);
        } else if (parts.length >= 3) {
            String fichaRaw = parts[parts.length - 1];
            fichaRaw = URLDecoder.decode(fichaRaw, StandardCharsets.UTF_8);
            
            // Store original fichaRaw for consistency
            sessionFichaRaw.put(session, fichaRaw);
            
            List<String> fichasReales = obtenerFichasReales(fichaRaw);
            for (String f : fichasReales) {
                sesionesPorFicha.computeIfAbsent(f, x -> new CopyOnWriteArrayList<>()).add(session);
            }
            // mark sessionSalas with empty list for consistency
            sessionSalas.put(session, List.of());
            logger.info("WS connect (aprendiz) session={} fichasReales={}", session.getId(), fichasReales);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MensajeGrupo mensaje = mapper.readValue(message.getPayload(), MensajeGrupo.class);
        logger.info("Incoming WS message session={} payloadFicha={} payloadSala={} texto='{}'", session.getId(), mensaje.getFicha(), mensaje.getSala(), mensaje.getMensaje());

        // Determine if sender is instructor or apprentice
        List<String> salas = sessionSalas.getOrDefault(session, List.of());
        logger.info("DEBUG: Session salas={}", salas);

        Set<String> targetSalas = new LinkedHashSet<>();
        String instructorGroupSala = null;  // For cross-ficha instructor group chat
        
        if (salas != null && !salas.isEmpty()) {
            // sender is instructor (connected to one or more salas) — use session salas, ignore payload sala
            targetSalas.addAll(salas);
            logger.info("DEBUG: Instructor sender, targetSalas={}", targetSalas);
        } else if (mensaje.getSala() != null && !mensaje.getSala().isBlank()) {
            // sender is apprentice with selected instructor
            String salaRaw = mensaje.getSala().trim();
            // NEW: sala format is now just instructorId (no ficha| prefix)
            if (!salaRaw.contains("|")) {
                // Group chat mode: instructor ID only
                instructorGroupSala = salaRaw;
                logger.info("DEBUG: Apprentice in group chat mode, instructorId={}", salaRaw);
            } else {
                // Backward compat: old format ficha|instructorId
                String[] parts = salaRaw.split("\\|", 2);
                String fichaPart = parts.length > 0 ? parts[0] : "";
                String instructorPart = parts.length > 1 ? parts[1] : "";
                List<String> fichasReales = obtenerFichasReales(fichaPart);
                for (String f : fichasReales) {
                    String s = f + "|" + instructorPart;
                    targetSalas.add(s);
                }
                logger.info("DEBUG: Apprentice old format, salaRaw={} targetSalas={}", salaRaw, targetSalas);
            }
        } else {
            // sender may be apprentice without selected instructor; broadcast to all salas that match the ficha
            String ficha = mensaje.getFicha();
            logger.info("DEBUG: Apprentice without instructor, ficha={}", ficha);
            if (ficha != null) {
                for (String salaKey : sesionesPorSala.keySet()) {
                    if (salaKey.startsWith(ficha + "|")) targetSalas.add(salaKey);
                }
            }
            logger.info("DEBUG: Found targetSalas={}", targetSalas);
        }

        // Persistence logic
        Map<String, MensajeGrupo> persistedPorSala = new LinkedHashMap<>();
        
        if (salas != null && !salas.isEmpty()) {
            // Instructor: persist with the ORIGINAL ficha range to preserve context
            String fichaOriginal = sessionFichaRaw.getOrDefault(session, "");
            
            // Get instructor ID from one of the salas (format: fichaReal|instructorId)
            String instructorId = "";
            if (!salas.isEmpty()) {
                String[] parts = salas.get(0).split("\\|");
                if (parts.length > 1) {
                    instructorId = parts[1];
                }
            }
            
            // Persist once with original ficha range
            if (!fichaOriginal.isEmpty() && !instructorId.isEmpty()) {
                String salaConRango = fichaOriginal + "|" + instructorId;
                
                MensajeGrupo msg = new MensajeGrupo();
                msg.setFicha(fichaOriginal);  // Store with range like "2996893 - 2996900"
                msg.setSala(salaConRango);
                msg.setMensaje(mensaje.getMensaje());
                msg.setIdEmisor(mensaje.getIdEmisor());
                msg.setNombreEmisor(mensaje.getNombreEmisor());
                msg.setRolEmisor(mensaje.getRolEmisor());
                MensajeGrupo p = mensajeService.guardarMensaje(msg);
                persistedPorSala.put(salaConRango, p);
                logger.info("Persisted instructor mensaje id={} sala={} ficha={} fecha={}", p.getIdMensaje(), salaConRango, fichaOriginal, p.getFechaEnvio());
            }
        } else if (instructorGroupSala != null) {
            // Apprentice in group chat: persist once with sala=instructorId (shared across fichas)
            String ficha = mensaje.getFicha();
            MensajeGrupo msg = new MensajeGrupo();
            msg.setFicha(ficha);
            msg.setSala(instructorGroupSala);  // Just instructor ID
            msg.setMensaje(mensaje.getMensaje());
            msg.setIdEmisor(mensaje.getIdEmisor());
            msg.setNombreEmisor(mensaje.getNombreEmisor());
            msg.setRolEmisor(mensaje.getRolEmisor());
            MensajeGrupo p = mensajeService.guardarMensaje(msg);
            persistedPorSala.put(instructorGroupSala, p);
            logger.info("Persisted apprentice GROUP CHAT mensaje id={} sala={} ficha={} fecha={}", p.getIdMensaje(), instructorGroupSala, ficha, p.getFechaEnvio());
        } else if (!targetSalas.isEmpty()) {
            // Apprentice old format: persist to target salas
            logger.info("DEBUG: Apprentice old format persisting to targetSalas={}", targetSalas);
            for (String sala : targetSalas) {
                String ficha = sala.split("\\|")[0];
                
                MensajeGrupo msg = new MensajeGrupo();
                msg.setFicha(ficha);
                msg.setSala(sala);
                msg.setMensaje(mensaje.getMensaje());
                msg.setIdEmisor(mensaje.getIdEmisor());
                msg.setNombreEmisor(mensaje.getNombreEmisor());
                msg.setRolEmisor(mensaje.getRolEmisor());
                MensajeGrupo p = mensajeService.guardarMensaje(msg);
                persistedPorSala.put(sala, p);
                logger.info("Persisted apprentice OLD FORMAT mensaje id={} sala={} ficha={} fecha={}", p.getIdMensaje(), sala, ficha, p.getFechaEnvio());
            }
        } else {
            logger.info("DEBUG: Apprentice message but NO targetSalas and NO instructorGroupSala, creating ficha-only entry");
            // Persist to ficha only (no sala) for peer-to-peer apprentice messages
            String ficha = mensaje.getFicha();
            if (ficha != null) {
                MensajeGrupo msg = new MensajeGrupo();
                msg.setFicha(ficha);
                msg.setSala(null);  // No sala, just ficha
                msg.setMensaje(mensaje.getMensaje());
                msg.setIdEmisor(mensaje.getIdEmisor());
                msg.setNombreEmisor(mensaje.getNombreEmisor());
                msg.setRolEmisor(mensaje.getRolEmisor());
                MensajeGrupo p = mensajeService.guardarMensaje(msg);
                persistedPorSala.put(ficha, p);  // Use ficha as key for broadcast purposes
                logger.info("Persisted apprentice (no instructor) mensaje id={} ficha={} fecha={}", p.getIdMensaje(), ficha, p.getFechaEnvio());
            }
        }

        // Build a single map session -> message (one message per session) to avoid duplicates
        Map<WebSocketSession, MensajeGrupo> sessionToMessage = new LinkedHashMap<>();

        logger.info("DEBUG: sessionToMessage building from persistedPorSala size={}", persistedPorSala.size());
        
        // First, assign sala subscribers (instructors)
        for (Map.Entry<String, MensajeGrupo> entry : persistedPorSala.entrySet()) {
            String sala = entry.getKey();
            MensajeGrupo msgEntry = entry.getValue();
            List<WebSocketSession> sesiones = sesionesPorSala.getOrDefault(sala, List.of());
            logger.info("DEBUG: Instructor sala={} has {} sessions", sala, sesiones.size());
            for (WebSocketSession s : sesiones) {
                if (!s.isOpen()) continue;
                sessionToMessage.putIfAbsent(s, msgEntry);
            }
        }
        
        // NEW: Assign group chat subscribers (apprentices in cross-ficha mode)
        if (instructorGroupSala != null) {
            MensajeGrupo msgEntry = persistedPorSala.get(instructorGroupSala);
            if (msgEntry != null) {
                List<WebSocketSession> sesionesGroup = sesionesPorInstructor.getOrDefault(instructorGroupSala, List.of());
                logger.info("DEBUG: Group chat sala={} has {} apprentice sessions", instructorGroupSala, sesionesGroup.size());
                for (WebSocketSession s : sesionesGroup) {
                    if (!s.isOpen()) continue;
                    sessionToMessage.putIfAbsent(s, msgEntry);
                }
            }
        }

        // Then, assign apprentice subscribers per ficha (for old format or ficha-only mode)
        for (Map.Entry<String, MensajeGrupo> entry : persistedPorSala.entrySet()) {
            String key = entry.getKey();  // May be sala (with |), ficha (without |), or instructorId (group chat)
            MensajeGrupo msgEntry = entry.getValue();
            
            // Skip instructor-only keys
            if (!key.contains("|") && !key.equals(instructorGroupSala)) {
                // It's a ficha-only key
                List<WebSocketSession> sesionesApr = sesionesPorFicha.getOrDefault(key, List.of());
                logger.info("DEBUG: Apprentice ficha-only key={} has {} sessions", key, sesionesApr.size());
                for (WebSocketSession s : sesionesApr) {
                    if (!s.isOpen()) continue;
                    sessionToMessage.putIfAbsent(s, msgEntry);
                }
            } else if (key.contains("|")) {
                // Old format sala: assign to apprentices with that ficha
                String fichaDeSala = key.split("\\|")[0];
                List<WebSocketSession> sesionesApr = sesionesPorFicha.getOrDefault(fichaDeSala, List.of());
                logger.info("DEBUG: Apprentice old-format sala={} ficha={} has {} sessions", key, fichaDeSala, sesionesApr.size());
                for (WebSocketSession s : sesionesApr) {
                    if (!s.isOpen()) continue;
                    sessionToMessage.putIfAbsent(s, msgEntry);
                }
            }
        }

        // Send one message per session
        for (Map.Entry<WebSocketSession, MensajeGrupo> e : sessionToMessage.entrySet()) {
            WebSocketSession s = e.getKey();
            MensajeGrupo m = e.getValue();
            try {
                s.sendMessage(new TextMessage(mapper.writeValueAsString(m)));
                logger.info("Sent persisted mensaje id={} to session={} (final delivery)", m.getIdMensaje(), s.getId());
            } catch (Exception ex) {
                // ignore
            }
        }
        
        // 🔔 Enviar notificaciones en tiempo real
        enviarNotificaciones(mensaje, persistedPorSala, fichasReales);
    }
    
    /**
     * Enviar notificaciones a aprendices e instructores
     */
    private void enviarNotificaciones(MensajeGrupo mensaje, Map<String, MensajeGrupo> persistedPorSala, List<String> fichasReales) {
        try {
            String nombreEmisor = mensaje.getNombreEmisor() != null ? mensaje.getNombreEmisor() : "Usuario";
            String rolEmisor = mensaje.getRolEmisor() != null ? mensaje.getRolEmisor() : "Desconocido";
            String textoMensaje = mensaje.getMensaje() != null ? 
                (mensaje.getMensaje().length() > 50 ? mensaje.getMensaje().substring(0, 50) + "..." : mensaje.getMensaje()) 
                : "(Sin mensaje)";
            
            // Notificar a aprendices de la ficha
            if (fichasReales != null && !fichasReales.isEmpty()) {
                for (String ficha : fichasReales) {
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setTipo("nuevo_mensaje");
                    notif.setTitulo("Nuevo mensaje de " + nombreEmisor);
                    notif.setMensaje(textoMensaje);
                    notif.setRemitente(nombreEmisor);
                    notif.setRolRemitente(rolEmisor);
                    notif.setFicha(ficha);
                    notif.setSonar(true);
                    
                    NotificationManager.notificarAprendicesDeFicha(ficha, notif);
                    logger.info("🔔 Notificación enviada a aprendices de ficha {}", ficha);
                }
            }
            
            // Notificar a instructores
            for (String sala : persistedPorSala.keySet()) {
                if (sala.contains("|")) {
                    String instructorId = sala.split("\\|")[1];
                    
                    NotificacionDTO notif = new NotificacionDTO();
                    notif.setTipo("nuevo_mensaje");
                    notif.setTitulo("Nuevo mensaje de " + nombreEmisor);
                    notif.setMensaje(textoMensaje);
                    notif.setRemitente(nombreEmisor);
                    notif.setRolRemitente(rolEmisor);
                    notif.setSala(sala);
                    notif.setSonar(true);
                    
                    NotificationManager.notificarInstructor(instructorId, notif);
                    logger.info("🔔 Notificación enviada a instructor {}", instructorId);
                }
            }
        } catch (Exception e) {
            logger.error("Error enviando notificaciones", e);
        }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        List<String> salas = sessionSalas.remove(session);
        if (salas != null && !salas.isEmpty()) {
            // Instructor session
            for (String sala : salas) {
                List<WebSocketSession> sesiones = sesionesPorSala.get(sala);
                if (sesiones != null) sesiones.remove(session);
            }
            logger.info("WS close (instructor) session={} salas={}", session.getId(), salas);
        } else {
            // Might be apprentice in group chat mode
            String instructorId = sessionInstructor.remove(session);
            if (instructorId != null) {
                List<WebSocketSession> sesiones = sesionesPorInstructor.get(instructorId);
                if (sesiones != null) sesiones.remove(session);
                logger.info("WS close (apprentice group chat) session={} instructorId={}", session.getId(), instructorId);
            }
        }
        // Also remove from any apprentice registrations
        for (List<WebSocketSession> lista : sesionesPorFicha.values()) {
            lista.remove(session);
        }
    }

    // ✅ Convierte "2996893 - 2996900" en ["2996893", "2996900"]
    private List<String> obtenerFichasReales(String ficha) {
        ficha = ficha.replace("–", "-");

        if (ficha.contains("-")) {
            String[] partes = ficha.split("-");
            List<String> lista = new ArrayList<>();
            for (String p : partes) lista.add(p.trim());
            return lista;
        }

        return List.of(ficha.trim());
    }
}
