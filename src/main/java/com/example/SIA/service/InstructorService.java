package com.example.SIA.service;

import com.example.SIA.entity.Instructor;
import com.example.SIA.repository.InstructorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class InstructorService {

    @Autowired
    private InstructorRepository instructorRepository;

    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).trim();
    }

    public Instructor buscarPorNombreNormalizado(String nombreExcel) {
        String normalizadoExcel = normalizar(nombreExcel);

        return instructorRepository.findAll().stream()
                .filter(i -> {
                    String nombreCompleto = i.getUsuario().getNombres() + " " + i.getUsuario().getApellidos();
                    return normalizar(nombreCompleto).equals(normalizadoExcel);
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Instructor no encontrado: " + nombreExcel));
    }

    public java.util.List<Instructor> obtenerTodos() {
        return instructorRepository.findAll();
    }
}