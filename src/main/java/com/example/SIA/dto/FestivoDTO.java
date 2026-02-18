package com.example.SIA.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// DTO: Objeto de Transferencia de Datos
// Esta clase sirve de "molde" para recibir los datos que vienen de afuera (JSON).
// Usamos @Data (Lombok) para no escribir getters y setters manualmente.
// @JsonIgnoreProperties(ignoreUnknown = true) evita errores si la API externa envía campos extra que no nos interesan.

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FestivoDTO {
    private String date; // Fecha del festivo (ej: "2025-01-01")
    private String localName; // Nombre en español (ej: "Año Nuevo")
    private String name; // Nombre en inglés
    private String countryCode; // Código del país (ej: "CO")
}
