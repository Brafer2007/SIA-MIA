package com.example.SIA.controller;

import com.example.SIA.entity.RespuestaEncuesta;
import com.example.SIA.repository.RespuestaEncuestaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class EncuestaController {

    @Autowired
    private RespuestaEncuestaRepository repo;

    @GetMapping("/encuesta")
    public String mostrarEncuesta() {
        return "encuesta";
    }

    @PostMapping("/encuesta/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardar(@RequestBody RespuestaEncuesta respuesta) {
        respuesta.setFechaRespuesta(java.time.LocalDateTime.now());
        repo.save(respuesta);
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/encuesta/instituciones")
    @ResponseBody
    public ResponseEntity<List<String>> buscarInstituciones(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(repo.findInstitucionesByQuery(q.trim()));
    }

    @GetMapping("/encuesta/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("total", repo.count());
        data.put("tipoInstitucion",   toMap(repo.countByTipoInstitucion()));
        data.put("asistenciaDigital", toMap(repo.countByAsistenciaDigital()));
        data.put("controlAcceso",     toMap(repo.countByControlAcceso()));
        data.put("comunicacion",      toMap(repo.countByComunicacion()));
        data.put("tareasDigital",     toMap(repo.countByTareasDigital()));
        data.put("certificados",      toMap(repo.countByCertificados()));
        data.put("mayorFalencia",     toMap(repo.countByMayorFalencia()));
        data.put("usariaSia",         toMap(repo.countByUsariaSia()));
        data.put("avgProblemasAsistencia", round(repo.avgProblemasAsistencia()));
        data.put("avgFacilidadTareas",     round(repo.avgFacilidadTareas()));
        data.put("avgSatisfaccionAdmin",   round(repo.avgSatisfaccionAdmin()));

        // Últimas 10 respuestas con comentario
        List<Map<String, String>> comentarios = new ArrayList<>();
        repo.findAll().stream()
            .filter(r -> r.getComentario() != null && !r.getComentario().isBlank())
            .sorted(Comparator.comparing(RespuestaEncuesta::getFechaRespuesta).reversed())
            .limit(10)
            .forEach(r -> {
                Map<String, String> c = new LinkedHashMap<>();
                c.put("fecha", r.getFechaRespuesta().toString().substring(0, 10));
                c.put("institucion", r.getNombreInstitucion() != null ? r.getNombreInstitucion() : (r.getTipoInstitucion() != null ? r.getTipoInstitucion() : "-"));
                c.put("comentario", r.getComentario());
                comentarios.add(c);
            });
        data.put("comentarios", comentarios);

        return ResponseEntity.ok(data);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "sin_respuesta";
            Long val = ((Number) row[1]).longValue();
            map.put(key, val);
        }
        return map;
    }

    private double round(Double val) {
        if (val == null) return 0;
        return Math.round(val * 10.0) / 10.0;
    }
}
