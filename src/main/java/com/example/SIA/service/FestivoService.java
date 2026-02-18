package com.example.SIA.service;

import com.example.SIA.dto.FestivoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Service
public class FestivoService {

    private static final Logger logger = LoggerFactory.getLogger(FestivoService.class);

    // Inyectamos la herramienta que configuramos en AppConfig
    @Autowired
    private RestTemplate restTemplate;

    // URL de la API pública gratuita (Nager.Date)
    // Documentación: https://date.nager.at/Api
    // Consultamos festivos de Colombia (CO) para el año actual (dinámico)
    private final String API_URL_TEMPLATE = "https://date.nager.at/api/v3/PublicHolidays/%d/CO";

    /**
     * Consume un Web Service externo para obtener los festivos.
     * 
     * @return Lista de festivos mapeados a nuestro DTO.
     */
    public List<FestivoDTO> obtenerFestivosDeColombia() {
        try {
            // Obtener el año actual dinámicamente
            int año = java.time.Year.now().getValue();
            String API_URL = String.format(API_URL_TEMPLATE, año);
            logger.info("Iniciando consumo de Web Service externo: {}", API_URL);

            // getForObject hace la magia:
            // 1. Va a la URL (GET)
            // 2. Descarga el JSON
            // 3. Convierte el JSON automáticamente a un array de nuestra clase FestivoDTO[]
            FestivoDTO[] respuesta = restTemplate.getForObject(API_URL, FestivoDTO[].class);

            if (respuesta != null) {
                logger.info("Se encontraron {} festivos en la API externa.", respuesta.length);
                return Arrays.asList(respuesta);
            } else {
                logger.warn("La API externa respondió pero no trajo datos.");
                return new ArrayList<>();
            }

        } catch (Exception e) {
            logger.error("Error fatal consumiendo el Web Service: {}", e.getMessage());
            // En la vida real, aquí podrías devolver una lista vacía o lanzar una excepción
            // personalizada
            return new ArrayList<>();
        }
    }
}
