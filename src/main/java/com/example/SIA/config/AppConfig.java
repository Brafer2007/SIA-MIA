package com.example.SIA.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // Registramos RestTemplate como un "Bean" disponible para toda la aplicación.
    // Esto es como comprar una herramienta (el cliente HTTP) y guardarla en la caja
    // de herramientas (Spring Context)
    // para que cualquier servicio (Service) pueda usarla cuando la necesite.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
