package com.tfi.gestion_congresos_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.tfi.gestion_congresos_backend.security.JwtAuthenticationFilter;

import lombok.*;;

///Clase de configuración spring security
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource; // Inyectamos la fuente de CORS

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Habilitamos CORS dentro de la cadena de filtros de seguridad
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // cada request debe autenticarse con JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Permitir explícitamente todas las peticiones OPTIONS preflight
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        /* 
                        .requestMatchers("/api/auth/**").permitAll() // Rutas públicas de autenticación
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll() // Registro de usuarios
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()  // Swagger / OpenAPI
                        .anyRequest().authenticated() // El resto requiere autenticación
                        */
                        .anyRequest().permitAll()
                        
                );
                // agregamos el filtro 
                //.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}