package com.example.SIA.service;

import com.example.SIA.entity.Usuario;
import com.example.SIA.observer.EventoCertificadoDescargado;
import com.example.SIA.observer.SistemaEventos;
import com.example.SIA.repository.UsuarioRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CertificadoService {

    private final UsuarioRepository usuarioRepository;

    // ============================================================
    // MÉTODO PRINCIPAL
    // ============================================================
    public byte[] generarCertificado(String tipo, Integer idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Observer
        SistemaEventos.emitir(new EventoCertificadoDescargado(usuario, tipo));

        return switch (tipo) {
            case "laboral" -> generarCertificadoLaboral(usuario);
            case "nomina" -> generarDesprendibleNomina(usuario);
            case "vinculacion" -> generarConstanciaVinculacion(usuario);
            case "aprendiz" -> generarCertificadoAprendiz(usuario);
            default -> throw new RuntimeException("Tipo de certificado no válido");
        };
    }

    // ============================================================
    // CERTIFICADO LABORAL
    // ============================================================
    private byte[] generarCertificadoLaboral(Usuario usuario) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezado(document, "CERTIFICADO LABORAL");

            document.add(new Paragraph(
                    "Se certifica que el señor(a) " +
                            usuario.getNombres() + " " + usuario.getApellidos() +
                            ", identificado(a) con documento No. " + usuario.getNoDocumento() +
                            ", se encuentra vinculado(a) a la institución.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12)
            ));

            agregarPie(document);
            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando certificado laboral", e);
        }
    }

    // ============================================================
    // DESPRENDIBLE DE NÓMINA
    // ============================================================
    private byte[] generarDesprendibleNomina(Usuario usuario) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezado(document, "DESPRENDIBLE DE NÓMINA");

            document.add(new Paragraph("Empleado: " + usuario.getNombres() + " " + usuario.getApellidos()));
            document.add(new Paragraph("Documento: " + usuario.getNoDocumento()));
            document.add(new Paragraph("Correo: " + usuario.getCorreo()));
            document.add(new Paragraph("Último pago: $1.500.000 COP"));

            agregarPie(document);
            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando desprendible de nómina", e);
        }
    }

    // ============================================================
    // CONSTANCIA DE VINCULACIÓN
    // ============================================================
    private byte[] generarConstanciaVinculacion(Usuario usuario) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            agregarEncabezado(document, "CONSTANCIA DE VINCULACIÓN");

            document.add(new Paragraph(
                    "Se certifica que " + usuario.getNombres() + " " + usuario.getApellidos() +
                            ", identificado(a) con documento No. " + usuario.getNoDocumento() +
                            ", se encuentra vinculado(a) a la institución.",
                    FontFactory.getFont(FontFactory.HELVETICA, 12)
            ));

            agregarPie(document);
            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando constancia de vinculación", e);
        }
    }

    // ============================================================
    // CERTIFICADO APRENDIZ (FORMATO SENA)
    // ============================================================
    private byte[] generarCertificadoAprendiz(Usuario usuario) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 60, 60, 60, 60);
            PdfWriter.getInstance(document, baos);
            document.open();

            // LOGO
            Image logo = Image.getInstance("src/main/resources/static/img/sena.png");
            logo.scaleToFit(90, 90);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);

            agregarCentrado(document, "SENA", 14, true);
            agregarCentrado(document, "REGIONAL DISTRITO CAPITAL", 10, false);
            agregarCentrado(document, "CENTRO DE SERVICIOS FINANCIEROS", 10, false);

            document.add(new Paragraph(" "));
            agregarCentrado(document, "HACE CONSTAR", 12, true);
            document.add(new Paragraph(" "));

            Paragraph texto = new Paragraph(
                    "Que " + usuario.getNombres().toUpperCase() + " " +
                            usuario.getApellidos().toUpperCase() +
                            ", identificado(a) con documento No. " +
                            usuario.getNoDocumento() +
                            ", se encuentra cursando el programa de formación " +
                            "TECNOLOGÍA EN ANÁLISIS Y DESARROLLO DE SOFTWARE, " +
                            "en modalidad presencial.",
                    FontFactory.getFont(FontFactory.HELVETICA, 11)
            );
            texto.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(texto);

            document.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3, 3, 3});

            agregarCelda(tabla, "DÍA", true);
            agregarCelda(tabla, "HORA INICIO", true);
            agregarCelda(tabla, "HORA FIN", true);

            agregarFila(tabla, "LUNES", "18:00", "22:00");
            agregarFila(tabla, "MARTES", "18:00", "22:00");
            agregarFila(tabla, "MIÉRCOLES", "18:00", "22:00");
            agregarFila(tabla, "JUEVES", "18:00", "22:00");
            agregarFila(tabla, "VIERNES", "18:00", "22:00");
            agregarFila(tabla, "SÁBADO", "16:00", "22:00");

            document.add(tabla);
            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "Se expide en BOGOTÁ a los " + new Date(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10)
            ));

            Image firma = Image.getInstance("src/main/resources/static/img/firma.jpg");
            firma.scaleToFit(120, 60);
            firma.setAlignment(Image.ALIGN_CENTER);
            document.add(firma);

            agregarCentrado(document,
                    "JORGE ORLANDO VALLEJO SUÁREZ\n" +
                            "SUBDIRECTOR (E) ENCARGADO(A)\n" +
                            "CENTRO DE SERVICIOS FINANCIEROS",
                    10, true);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando certificado aprendiz", e);
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================
    private void agregarEncabezado(Document document, String titulo) throws Exception {
        Paragraph p = new Paragraph(titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
        document.add(new Paragraph(" "));
    }

    private void agregarPie(Document document) throws Exception {
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Fecha de emisión: " + new Date(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));
    }

    private void agregarCelda(PdfPTable tabla, String texto, boolean header) {
        Font font = header
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
                : FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        tabla.addCell(cell);
    }

    private void agregarFila(PdfPTable tabla, String dia, String inicio, String fin) {
        agregarCelda(tabla, dia, false);
        agregarCelda(tabla, inicio, false);
        agregarCelda(tabla, fin, false);
    }

    private void agregarCentrado(Document document, String texto, int size, boolean bold)
            throws Exception {
        Font font = bold
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, size)
                : FontFactory.getFont(FontFactory.HELVETICA, size);

        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);
    }
}
