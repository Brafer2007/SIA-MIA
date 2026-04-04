package com.example.SIA.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                // Evita que el sitio sea embebido en iframes (clickjacking)
                .frameOptions(frame -> frame.deny())
                // Fuerza HTTPS
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                // Evita sniffing de MIME types
                .contentTypeOptions(ct -> {})
                // Oculta información del servidor
                .and()
                .addHeaderWriter((req, res) -> {
                    // Elimina cabeceras que revelan tecnología
                    res.setHeader("X-Powered-By", "");
                    res.setHeader("Server", "");
                    // Política de referrer
                    res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                    // Permisos del navegador
                    res.setHeader("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=()");
                    // Content Security Policy — permite recursos propios + CDNs usados
                    res.setHeader("Content-Security-Policy",
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                            "https://cdn.jsdelivr.net https://code.jquery.com " +
                            "https://cdn.datatables.net https://unpkg.com; " +
                        "style-src 'self' 'unsafe-inline' " +
                            "https://fonts.googleapis.com " +
                            "https://cdnjs.cloudflare.com " +
                            "https://cdn.datatables.net; " +
                        "font-src 'self' https://fonts.gstatic.com " +
                            "https://cdnjs.cloudflare.com; " +
                        "img-src 'self' data: blob: https:; " +
                        "connect-src 'self' wss: ws:; " +
                        "frame-ancestors 'none';"
                    );
                })
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
