package com.example.SIA.controller;

import com.example.SIA.dto.EnviarCorreoDTO;
import com.example.SIA.entity.Usuario;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.service.EmailService;
import com.example.SIA.service.UsuarioService;
import com.example.SIA.service.AprendizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private AprendizService aprendizService;
    
    /**
     * Endpoint para enviar correos masivos desde el perfil del instructor
     * 
     * POST http://localhost:8080/api/email/enviar-correos
     * Body JSON:
     * {
     *   "destinatarios": ["email1@example.com", "email2@example.com"],
     *   "asunto": "Tema del correo",
     *   "mensaje": "Contenido del correo"
     * }
     */
    @PostMapping("/enviar-correos")
    public ResponseEntity<Map<String, Object>> enviarCorreos(
            @RequestBody EnviarCorreoDTO request,
            HttpSession session) {
        
        Map<String, Object> respuesta = new HashMap<>();
        
        try {
            // Verificar que el usuario sea instructor
            Integer idUsuario = (Integer) session.getAttribute("idUsuario");
            Integer perfil = (Integer) session.getAttribute("id_perfil");
            
            if (idUsuario == null || perfil == null || perfil != 3) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "Acceso denegado. Solo instructores pueden enviar correos masivos.");
                return ResponseEntity.status(403).body(respuesta);
            }
            
            // Obtener el email del instructor
            Usuario usuario = usuarioService.findById(idUsuario);
            String emailInstructor = usuario.getCorreo();
            
            // Validar datos
            if (request.getDestinatarios() == null || request.getDestinatarios().isEmpty()) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "Debe seleccionar al menos un destinatario.");
                return ResponseEntity.badRequest().body(respuesta);
            }
            
            if (request.getAsunto() == null || request.getAsunto().trim().isEmpty()) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "El asunto no puede estar vacío.");
                return ResponseEntity.badRequest().body(respuesta);
            }
            
            if (request.getMensaje() == null || request.getMensaje().trim().isEmpty()) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "El mensaje no puede estar vacío.");
                return ResponseEntity.badRequest().body(respuesta);
            }
            
            // Enviar correo masivo
            boolean enviado = emailService.enviarCorreoMasivo(
                    request.getDestinatarios(),
                    request.getAsunto(),
                    request.getMensaje(),
                    emailInstructor
            );
            
            if (enviado) {
                respuesta.put("exito", true);
                respuesta.put("mensaje", "Correos enviados exitosamente a " + request.getDestinatarios().size() + " destinatarios.");
                respuesta.put("cantidad", request.getDestinatarios().size());
                logger.info("Instructor {} envió correos masivos a {} destinatarios", idUsuario, request.getDestinatarios().size());
                return ResponseEntity.ok(respuesta);
            } else {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "Error al enviar los correos. Intenta de nuevo.");
                return ResponseEntity.status(500).body(respuesta);
            }
            
        } catch (Exception e) {
            logger.error("Error en envío de correos: {}", e.getMessage(), e);
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(500).body(respuesta);
        }
    }
    
    /**
     * Endpoint para obtener lista de aprendices y sus emails
     * Usado para llenar el formulario en el dashboard
     * 
     * GET /api/email/aprendices
     */
    @GetMapping("/aprendices")
    public ResponseEntity<Map<String, Object>> obtenerAprendices(HttpSession session) {
        
        Map<String, Object> respuesta = new HashMap<>();
        
        try {
            // Verificar que sea instructor
            Integer perfil = (Integer) session.getAttribute("id_perfil");
            
            if (perfil == null || perfil != 3) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "Acceso denegado.");
                return ResponseEntity.status(403).body(respuesta);
            }
            
            // Obtener todos los aprendices y sus emails
            // Nota: AprendizService solo tiene findByUsuarioId, así que buscamos de manera diferente
            // Alternativa: obtener todos los usuarios con perfil de aprendiz
            List<Usuario> usuarios = usuarioService.findAll();
            
            List<Map<String, String>> aprendicesDTO = usuarios.stream()
                    .filter(u -> u.getPerfil() != null && u.getPerfil().getIdPerfil() == 1) // perfil 1 = aprendiz
                    .filter(u -> u.getCorreo() != null && !u.getCorreo().isEmpty())
                    .map(u -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("email", u.getCorreo());
                        map.put("nombre", u.getNombres() + " " + u.getApellidos());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            respuesta.put("exito", true);
            respuesta.put("aprendices", aprendicesDTO);
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            logger.error("Error al obtener aprendices: {}", e.getMessage(), e);
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Error al obtener lista de aprendices.");
            return ResponseEntity.status(500).body(respuesta);
        }
    }
}
