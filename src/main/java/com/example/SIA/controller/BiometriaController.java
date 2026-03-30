package com.example.SIA.controller;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.SIA.service.UsuarioService;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/biometria")
@CrossOrigin(origins = "*") // Permite llamadas desde el Windows Service
public class BiometriaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ===============================
    // Guardar huella en base de datos
    // ===============================
    @PostMapping("/guardar/{idUsuario}")
    public ResponseEntity<?> guardarHuella(
            @PathVariable Integer idUsuario,
            @RequestBody Map<String, String> body) {

        try {

            Optional<Usuario> optionalUsuario = usuarioRepository.findById(idUsuario);

            if (optionalUsuario.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            Usuario usuario = optionalUsuario.get();

            // Convertir Base64 a bytes
            String huellaBase64 = body.get("huella");

            if (huellaBase64 == null || huellaBase64.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Huella vacía"));
            }

            byte[] huellaBytes = Base64.getDecoder().decode(huellaBase64);

            usuario.setHuella(huellaBytes);

            usuarioRepository.save(usuario);

            return ResponseEntity.ok(
                    Map.of("success", true, "message", "Huella guardada correctamente"));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Error interno", "error", e.getMessage()));
        }
    }
}