package com.example.SIA.controller;

import com.example.SIA.entity.Usuario;
import com.example.SIA.service.CertificadoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    // ============================================================
    // DESCARGA CERTIFICADO - APRENDIZ
    // ============================================================
    @GetMapping("/aprendiz/{tipo}")
    public ResponseEntity<byte[]> descargarCertificadoAprendiz(
            @PathVariable String tipo,
            HttpSession session) {

        // 🔑 Obtener usuario desde sesión (MISMO NOMBRE QUE EN LOGIN)
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        // ❌ No hay sesión
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        // ❌ No es aprendiz
        if (!"Aprendiz".equalsIgnoreCase(usuario.getPerfil().getNombrePerfil())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = certificadoService.generarCertificado(tipo, usuario.getIdUsuario());

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificado-aprendiz.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ============================================================
    // DESCARGA CERTIFICADO - ADMIN / INSTRUCTOR / ADMINISTRATIVO
    // ============================================================
    @GetMapping("/{tipo}/{idUsuario}")
    public ResponseEntity<byte[]> descargarCertificadoPorUsuario(
            @PathVariable String tipo,
            @PathVariable Integer idUsuario,
            HttpSession session) {

        Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");

        // ❌ No hay sesión
        if (usuarioSesion == null) {
            return ResponseEntity.status(401).build();
        }

        String perfil = usuarioSesion.getPerfil().getNombrePerfil();

        // ❌ Perfil no autorizado
        if (!perfil.equalsIgnoreCase("Administrador")
                && !perfil.equalsIgnoreCase("Instructor")
                && !perfil.equalsIgnoreCase("Administrativo")) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = certificadoService.generarCertificado(tipo, idUsuario);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + tipo + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
