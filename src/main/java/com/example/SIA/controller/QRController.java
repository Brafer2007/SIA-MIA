package com.example.SIA.controller;

import com.example.SIA.entity.Equipo;
import com.example.SIA.entity.Usuario;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.repository.EquipoRepository;
import com.example.SIA.repository.AprendizRepository;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/aprendiz")
public class QRController {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private AprendizRepository aprendizRepository;

    @GetMapping("/ver-qr")
    public String verQR(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);
        model.addAttribute("nombreQR", generarNombreArchivo(usuario));
        return "aprendiz/verQR";
    }

    @GetMapping("/qr-img")
    public ResponseEntity<Resource> generarQR(HttpSession session) throws Exception {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            throw new RuntimeException("Usuario no autenticado");
        }

        List<Equipo> equipos = equipoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        List<Aprendiz> listaAprendiz = aprendizRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        Aprendiz aprendiz = listaAprendiz.isEmpty() ? null : listaAprendiz.get(0);

        String contenidoQR = construirContenidoQR(usuario, aprendiz, equipos);

        Path qrDir = Paths.get(System.getProperty("user.dir"), "qr-codes");
        Files.createDirectories(qrDir);
        Path qrPath = qrDir.resolve("qr_" + usuario.getIdUsuario() + ".png");

        BitMatrix matrix = new MultiFormatWriter().encode(contenidoQR, BarcodeFormat.QR_CODE, 250, 250);
        try (OutputStream os = Files.newOutputStream(qrPath)) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        }

        // ✅ Aplicar logo como fondo transparente
        aplicarLogoComoFondo(qrPath, "static/img/logo_sia_fondo.png");

        Resource file = new UrlResource(qrPath.toUri());

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + generarNombreArchivo(usuario) + "\"")
            .body(file);
    }

    private String construirContenidoQR(Usuario usuario, Aprendiz aprendiz, List<Equipo> equipos) throws Exception {
        StringBuilder sb = new StringBuilder();

        sb.append("🟢 SOFTWARE DE INGRESO Y AUTOMATIZACIÓN\n");
        sb.append("───────────────────────────────────────\n");
        sb.append("👤 *Nombre:* ").append(usuario.getNombres()).append(" ").append(usuario.getApellidos()).append("\n");
        sb.append("🆔 *Documento:* ").append(usuario.getNoDocumento()).append("\n");
        sb.append("📧 *Correo:* ").append(usuario.getCorreo()).append("\n");

        if (aprendiz != null) {
            sb.append("📚 *Ficha:* ").append(aprendiz.getFichaFormacion()).append("\n");
            sb.append("🎓 *Programa:* ").append(aprendiz.getProgramaFormacion()).append("\n");
        }

        sb.append("💻 *Equipos asignados:*\n");
        for (Equipo eq : equipos) {
            sb.append("• ").append(eq.getTipoEquipo()).append(" | ")
              .append(eq.getMarcaModelo()).append(" | ")
              .append(eq.getNumeroSerie()).append(" | ")
              .append(eq.getEstado() == 1 ? "Activo" : "Inactivo").append("\n");
        }

        sb.append("───────────────────────────────────────\n");
        sb.append("🔐 Firma digital: ").append(firmarContenido(sb.toString())).append("\n");

        return sb.toString();
    }

    private String generarNombreArchivo(Usuario usuario) {
        String nombre = usuario.getNombres() + "_" + usuario.getApellidos() + "_" + usuario.getNoDocumento();
        nombre = nombre.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "");
        return "QR_" + nombre + ".png";
    }

    private void aplicarLogoComoFondo(Path qrPath, String logoPath) throws Exception {
        BufferedImage qrImage = ImageIO.read(qrPath.toFile());
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream(logoPath);
        if (logoStream == null) throw new IllegalStateException("No se encontró el logo en: " + logoPath);

        BufferedImage logo = ImageIO.read(logoStream);
        BufferedImage resizedLogo = escalarLogo(logo, qrImage.getWidth(), qrImage.getHeight());

        BufferedImage combinado = new BufferedImage(qrImage.getWidth(), qrImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinado.createGraphics();

        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.15f));
        g.drawImage(resizedLogo, 0, 0, null);

        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
        g.drawImage(qrImage, 0, 0, null);

        g.dispose();
        ImageIO.write(combinado, "PNG", qrPath.toFile());
    }

    private BufferedImage escalarLogo(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private String firmarContenido(String contenido) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(obtenerClavePrivada());
        signature.update(contenido.getBytes(StandardCharsets.UTF_8));
        byte[] firma = signature.sign();
        return Base64.getEncoder().encodeToString(firma);
    }

    private PrivateKey obtenerClavePrivada() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("keys/private.pem");
        if (is == null) throw new IllegalStateException("No se encontró el archivo private.pem");

        byte[] keyBytes = is.readAllBytes();
        String privateKeyPEM = new String(keyBytes)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}