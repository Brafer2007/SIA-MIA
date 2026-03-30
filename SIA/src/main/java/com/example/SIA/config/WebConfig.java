package com.example.SIA.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
// 🟠 PATRÓN ADAPTER - Implementa WebMvcConfigurer para adaptarse a Spring
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private NoCacheInterceptor noCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 🔶 PATRÓN DECORATOR - El interceptor DECORA las peticiones HTTP
        // Agrega headers de no-cache de forma automática y transparente
        registry.addInterceptor(noCacheInterceptor)
                .addPathPatterns("/dashboard/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir archivos desde el directorio /uploads
        String uploadDir = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}