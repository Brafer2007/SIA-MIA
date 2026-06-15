package com.example.SIA.config;

import com.example.SIA.service.GoogleOAuth2UserService;
import com.example.SIA.service.GoogleOidcUserService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final GoogleOidcUserService googleOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    public SecurityConfig(GoogleOAuth2UserService googleOAuth2UserService,
                          GoogleOidcUserService googleOidcUserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          OAuth2FailureHandler oAuth2FailureHandler) {
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.googleOidcUserService = googleOidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }

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
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    // OAuth2 puro (sin openid) — fallback
                    .userService(googleOAuth2UserService)
                    // OIDC (con openid) — lo que Google usa siempre
                    .oidcUserService(googleOidcUserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .contentTypeOptions(ct -> {})
            );

        return http.build();
    }

    /** Filtro separado para headers de seguridad adicionales */
    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((ServletRequest req, ServletResponse res, FilterChain chain) -> {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setHeader("X-Powered-By", "");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "camera=*, microphone=(), geolocation=()");
            response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                    "https://cdn.jsdelivr.net https://code.jquery.com " +
                    "https://cdn.datatables.net https://unpkg.com; " +
                "style-src 'self' 'unsafe-inline' " +
                    "https://fonts.googleapis.com https://cdnjs.cloudflare.com " +
                    "https://cdn.datatables.net; " +
                "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                "img-src 'self' data: blob: https:; " +
                "connect-src 'self' wss: ws:; " +
                "frame-ancestors 'none';"
            );
            chain.doFilter(req, res);
        });
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
