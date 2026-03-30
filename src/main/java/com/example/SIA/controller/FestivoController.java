package com.example.SIA.controller;

import com.example.SIA.dto.FestivoDTO;
import com.example.SIA.service.FestivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/externos")
public class FestivoController {

    @Autowired
    private FestivoService festivoService;

    // Endpoint para probar nuestra integración
    // URL: http://localhost:8080/api/externos/festivos
    @GetMapping("/festivos")
    public ResponseEntity<?> obtenerFestivos() {

        List<FestivoDTO> festivos = festivoService.obtenerFestivosDeColombia();

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("origen", "API Externa (date.nager.at)");
        respuesta.put("cantidad", festivos.size());
        respuesta.put("datos", festivos);

        return ResponseEntity.ok(respuesta);
    }
}
