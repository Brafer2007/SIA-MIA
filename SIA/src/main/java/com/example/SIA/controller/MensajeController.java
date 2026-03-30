package com.example.SIA.controller;

import com.example.SIA.entity.MensajeGrupo;
import com.example.SIA.entity.Programacion;
import com.example.SIA.repository.ProgramacionRepository;
import com.example.SIA.service.MensajeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;
    private final ProgramacionRepository programacionRepository;

    public MensajeController(MensajeService mensajeService, ProgramacionRepository programacionRepository) {
        this.mensajeService = mensajeService;
        this.programacionRepository = programacionRepository;
    }

    @GetMapping("/ficha/{ficha}/{instructorId}")
    public ResponseEntity<List<MensajeGrupo>> obtenerMensajes(@PathVariable String ficha, @PathVariable String instructorId) {
        List<MensajeGrupo> mensajes = new java.util.ArrayList<>();
        
        // Normalize ficha
        String fichaNorm = ficha.replace("–", "-");
        
        if (fichaNorm.contains("-")) {
            // It's a range like "2996893 - 2996900" (instructor view)
            
            // First, search for instructor's messages with full range
            String salaExacta = fichaNorm + "|" + instructorId;
            mensajes.addAll(mensajeService.obtenerMensajesPorSala(salaExacta));
            
            // Also search for apprentice messages from individual fichas in the range
            String[] fichasEnRango = fichaNorm.split("-");
            for (String f : fichasEnRango) {
                f = f.trim();
                String salaAprendiz = f + "|" + instructorId;
                mensajes.addAll(mensajeService.obtenerMensajesPorSala(salaAprendiz));
            }
        } else {
            // It's a single ficha - apprentice searching
            // Find the programacion entry that contains this ficha + instructor
            Long instructorIdLong = Long.parseLong(instructorId);
            List<Programacion> programaciones = programacionRepository.findByInstructorId(instructorIdLong);
            
            // Find the range that contains this ficha
            java.util.Set<String> fichasEnMismoRango = new java.util.HashSet<>();
            for (Programacion p : programaciones) {
                String nomFicha = p.getNombreFicha();
                if (nomFicha == null) continue;
                nomFicha = nomFicha.replace("–", "-");
                String[] partes = nomFicha.split("-");
                // Check if this range contains our ficha
                for (String parte : partes) {
                    if (parte.trim().equalsIgnoreCase(fichaNorm)) {
                        // Found the range containing our ficha
                        // Add all fichas in this range
                        for (String p2 : partes) {
                            fichasEnMismoRango.add(p2.trim());
                        }
                        break;
                    }
                }
            }
            
            // If no range found, just use the ficha provided
            if (fichasEnMismoRango.isEmpty()) {
                fichasEnMismoRango.add(fichaNorm);
            }
            
            // Search messages from fichas in the same range
            for (String f : fichasEnMismoRango) {
                List<MensajeGrupo> porFicha = mensajeService.obtenerMensajesPorFicha(f);
                // Filter to only messages from this instructor or aprendices of this instructor in this range
                for (MensajeGrupo m : porFicha) {
                    if (m.getSala() != null && m.getSala().contains("|" + instructorId)) {
                        mensajes.add(m);
                    }
                }
            }
        }
        
        // Remove duplicates and sort
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<MensajeGrupo> deduped = new java.util.ArrayList<>();
        for (MensajeGrupo m : mensajes) {
            String key = m.getIdEmisor() + "|" + m.getMensaje() + "|" + m.getFechaEnvio();
            if (!seen.contains(key)) {
                seen.add(key);
                deduped.add(m);
            }
        }
        
        deduped.sort((a, b) -> {
            if (a.getFechaEnvio() == null || b.getFechaEnvio() == null) return 0;
            return a.getFechaEnvio().compareTo(b.getFechaEnvio());
        });
        
        return ResponseEntity.ok(deduped);
    }

    @GetMapping("/ficha/{ficha}")
    public ResponseEntity<List<MensajeGrupo>> obtenerMensajesPorFicha(@PathVariable String ficha) {
        // Get all messages for this ficha (from any instructor or no instructor)
        List<MensajeGrupo> mensajes = mensajeService.obtenerMensajesPorFicha(ficha);
        
        // Sort by fecha
        mensajes.sort((a, b) -> {
            if (a.getFechaEnvio() == null || b.getFechaEnvio() == null) return 0;
            return a.getFechaEnvio().compareTo(b.getFechaEnvio());
        });
        
        return ResponseEntity.ok(mensajes);
    }

    @GetMapping("/chats-por-ficha/{ficha}")
    public ResponseEntity<List<String>> obtenerChatsPorFicha(@PathVariable String ficha) {
        // Buscar programaciones que incluyan la ficha (usa LIKE en repo)
        List<Programacion> lista = programacionRepository.findByNombreFicha(ficha);
        java.util.Set<String> salas = new java.util.LinkedHashSet<>();
        for (Programacion p : lista) {
            String nombre = p.getNombreFicha();
            if (nombre == null) continue;
            nombre = nombre.replace("–", "-");
            String[] partes = nombre.split("-");
            for (String parte : partes) {
                if (parte.trim().equalsIgnoreCase(ficha.trim())) {
                    if (p.getInstructor() != null) {
                        salas.add(parte.trim() + "|" + p.getInstructor().getId());
                    }
                }
            }
        }
        return ResponseEntity.ok(new java.util.ArrayList<>(salas));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "El archivo está vacío");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Usar directorio de trabajo actual del proyecto
            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
            Files.createDirectories(Paths.get(uploadDir));
            
            // Generar nombre único para el archivo
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".") ? 
                originalName.substring(originalName.lastIndexOf(".")) : "";
            String uniqueName = UUID.randomUUID().toString() + ext;
            
            // Guardar el archivo
            String filePath = uploadDir + File.separator + uniqueName;
            file.transferTo(new File(filePath));
            
            // Construir URL pública
            String fileUrl = "/uploads/" + uniqueName;
            
            response.put("success", true);
            response.put("url", fileUrl);
            response.put("fileName", originalName);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error al guardar archivo: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    @PostMapping("/guardar")
    public ResponseEntity<MensajeGrupo> guardarMensaje(@RequestBody MensajeGrupo mensaje) {
        if (mensaje.getFicha() == null || mensaje.getFicha().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Si no tiene sala, la dejamos null
        MensajeGrupo saved = mensajeService.guardarMensaje(mensaje);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/debug/todos")
    public ResponseEntity<List<MensajeGrupo>> debug() {
        return ResponseEntity.ok(mensajeService.obtenerTodosMensajes());
    }

    // NUEVO ENDPOINT: Obtener mensajes solo de las fichas asociadas a la sala de un instructor
    @GetMapping("/sala/{instructorId}")
    public ResponseEntity<List<MensajeGrupo>> obtenerMensajesPorSalaDeInstructor(@PathVariable Long instructorId) {
        List<Programacion> programaciones = programacionRepository.findByInstructorId(instructorId);
        java.util.Set<String> fichas = programaciones.stream()
            .map(Programacion::getNombreFicha)
            .collect(java.util.stream.Collectors.toSet());

        java.util.List<MensajeGrupo> mensajes = new java.util.ArrayList<>();
        for (String ficha : fichas) {
            mensajes.addAll(mensajeService.obtenerMensajesPorFicha(ficha));
        }
        // Ordenar por fecha
        mensajes.sort(java.util.Comparator.comparing(MensajeGrupo::getFechaEnvio));
        return ResponseEntity.ok(mensajes);
    }
}
