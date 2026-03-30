package com.example.SIA.service;

import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.EquipoRepository;
import com.example.SIA.repository.UsuarioRepository;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AprendizRepository aprendizRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;    

    // Color institucional SENA
    private static final Color COLOR_SENA = new Color(0, 107, 45);

    // ================================================================
    // UTILIDADES GENERALES
    // ================================================================

    private void agregarLogo(Document document) throws IOException, DocumentException {
        try {
            // Intenta cargar logo_sia_fondo.png desde classpath
            ClassPathResource resource = new ClassPathResource("static/img/logo_sia_fondo.png");
            if (resource.exists()) {
                InputStream inputStream = resource.getInputStream();
                byte[] logoBytes = inputStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Image.ALIGN_LEFT);
                document.add(logo);
                inputStream.close();
            }
        } catch (Exception e) {
            // Si falla, continúa sin logo (mejor que fallar completamente)
            System.out.println("Advertencia: No se pudo cargar el logo: " + e.getMessage());
        }
    }

    private void agregarTitulo(Document document, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(
                titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_SENA)
        );
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);

        Paragraph fecha = new Paragraph(
                "Generado: " + new Date(),
                FontFactory.getFont(FontFactory.HELVETICA, 10)
        );
        fecha.setAlignment(Element.ALIGN_RIGHT);
        document.add(fecha);

        document.add(new Paragraph(" "));
    }

    private PdfPTable crearTablaEstilizada(int columnas) {
        PdfPTable table = new PdfPTable(columnas);
        table.setWidthPercentage(100);
        return table;
    }

    private PdfPCell celdaHeader(String texto) {
        PdfPCell cell = new PdfPCell(
                new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE))
        );
        cell.setBackgroundColor(COLOR_SENA);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell celdaNormal(String texto) {
        PdfPCell cell = new PdfPCell(
                new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK))
        );
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        return cell;
    }

    private Image chartToPdfImage(JFreeChart chart, int width, int height)
            throws IOException, BadElementException {
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
        ChartUtils.writeBufferedImageAsPNG(chartBaos, bufferedImage);
        return Image.getInstance(chartBaos.toByteArray());
    }

    // ================================================================
    // DATOS PARA GRÁFICAS (JSON PARA FRONT)
    // ================================================================

    public Map<String, Object> getAprendicesPorPrograma() {
        Map<String, Integer> conteo = new LinkedHashMap<>();

        aprendizRepository.findAll().forEach(a -> {
            String programa = a.getProgramaFormacion();
            conteo.put(programa, conteo.getOrDefault(programa, 0) + 1);
        });

        return Map.of(
                "labels", conteo.keySet(),
                "values", conteo.values()
        );
    }

    public Map<String, Long> getUsuariosActivosInactivos() {
        long activos = usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != null && u.getEstado() == 1)
                .count();

        long inactivos = usuarioRepository.findAll().stream()
                .filter(u -> u.getEstado() != null && u.getEstado() == 0)
                .count();

        // Usamos claves "Activos"/"Inactivos" para que se vean así en tabla y gráfica
        return Map.of(
                "Activos", activos,
                "Inactivos", inactivos
        );
    }

    public Map<String, Long> getEquiposPorTipo() {
        List<Object[]> data = equipoRepository.countByTipoEquipo();

        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Object[] row : data) {
            String tipo = row[0] != null ? row[0].toString() : "Sin tipo";
            Long total = (Long) row[1];
            mapa.put(tipo, total);
        }

        return mapa;
    }

    // ================================================================
    // GRÁFICAS PARA PDF
    // ================================================================

    // Barras: Aprendices por programa
    private Image generarGraficaBarrasAprendices(Map<String, Integer> datos)
            throws IOException, BadElementException {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        datos.forEach((k, v) -> dataset.addValue(v.doubleValue(), "Aprendices", k));

        JFreeChart chart = ChartFactory.createBarChart(
                "Aprendices por programa",
                "Programa",
                "Total",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(COLOR_SENA);

        return chartToPdfImage(chart, 500, 300);
    }

    // Pie: Usuarios activos / inactivos
    private Image generarGraficaPieUsuarios(Map<String, Long> datos)
            throws IOException, BadElementException {

        DefaultPieDataset dataset = new DefaultPieDataset();

        datos.forEach((k, v) -> dataset.setValue(k, v.doubleValue()));

        JFreeChart chart = ChartFactory.createPieChart(
                "Usuarios por estado",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setSectionPaint("Activos", COLOR_SENA);
        plot.setSectionPaint("Inactivos", Color.GRAY);

        return chartToPdfImage(chart, 500, 300);
    }

    // Doughnut (simulado con pie): Equipos por tipo
    private Image generarGraficaDoughnutEquipos(Map<String, Long> datos)
            throws IOException, BadElementException {

        DefaultPieDataset dataset = new DefaultPieDataset();
        datos.forEach((k, v) -> dataset.setValue(k, v.doubleValue()));

        JFreeChart chart = ChartFactory.createPieChart(
                "Equipos por tipo",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setCircular(true);
        plot.setLabelBackgroundPaint(Color.WHITE);
        // Simular doughnut
        plot.setInteriorGap(0.40);

        return chartToPdfImage(chart, 500, 300);
    }

    // ================================================================
    // PDF: APRENDICES POR PROGRAMA
    // ================================================================

    // PATRÓN TEMPLATE METHOD

    public byte[] generarPdfAprendicesPorPrograma() {
        try {
            // Construimos el mapa de datos (para tabla y gráfica)
            Map<String, Integer> datos = new LinkedHashMap<>();
            aprendizRepository.findAll().forEach(a -> {
                String programa = a.getProgramaFormacion();
                datos.put(programa, datos.getOrDefault(programa, 0) + 1);
            });

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            agregarLogo(document);                                   
            agregarTitulo(document, "REPORTE: APRENDICES POR PROGRAMA"); 

            // Gráfica
            if (!datos.isEmpty()) {
                Image grafica = generarGraficaBarrasAprendices(datos); 
                grafica.setAlignment(Element.ALIGN_CENTER);
                document.add(grafica);
                document.add(new Paragraph(" "));
            }

            // Tabla
            PdfPTable table = crearTablaEstilizada(2);
            table.addCell(celdaHeader("Programa"));
            table.addCell(celdaHeader("Total"));

            datos.forEach((k, v) -> {
                table.addCell(celdaNormal(k));
                table.addCell(celdaNormal(v.toString()));
            });

            document.add(table);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de aprendices por programa", e);
        }
    }

    // ================================================================
    // PDF: EQUIPOS POR TIPO
    // ================================================================

    public byte[] generarPdfEquipos() {
        try {
            Map<String, Long> datos = getEquiposPorTipo();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            agregarLogo(document);
            agregarTitulo(document, "REPORTE: EQUIPOS POR TIPO");

            // Gráfica doughnut (simulada)
            if (!datos.isEmpty()) {
                Image grafica = generarGraficaDoughnutEquipos(datos);
                grafica.setAlignment(Element.ALIGN_CENTER);
                document.add(grafica);
                document.add(new Paragraph(" "));
            }

            // Tabla
            PdfPTable table = crearTablaEstilizada(2);
            table.addCell(celdaHeader("Tipo de Equipo"));
            table.addCell(celdaHeader("Total"));

            datos.forEach((k, v) -> {
                table.addCell(celdaNormal(k));
                table.addCell(celdaNormal(v.toString()));
            });

            document.add(table);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de equipos", e);
        }
    }

    // ================================================================
    // PDF: USUARIOS POR ESTADO
    // ================================================================

    public byte[] generarPdfUsuariosEstado() {
        try {
                Map<String, Long> datos = getUsuariosActivosInactivos();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, baos);

                document.open();

                agregarLogo(document);
                agregarTitulo(document, "REPORTE: ESTADO DE USUARIOS");

                // Gráfica pie
                if (!datos.isEmpty()) {
                    Image grafica = generarGraficaPieUsuarios(datos);
                    grafica.setAlignment(Element.ALIGN_CENTER);
                    document.add(grafica);
                    document.add(new Paragraph(" "));
                }

                // Tabla
                PdfPTable table = crearTablaEstilizada(2);
                table.addCell(celdaHeader("Estado"));
                table.addCell(celdaHeader("Total"));

                datos.forEach((k, v) -> {
                    table.addCell(celdaNormal(k));
                    table.addCell(celdaNormal(v.toString()));
                });

                document.add(table);
                document.close();

                return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de usuarios por estado", e);
        }
    }
}