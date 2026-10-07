package com.tfi.gestion_congresos_backend.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.UserRepository;

import io.jsonwebtoken.JwtException; // 👈 nuevo import
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CongressParticipantRepository participantRepository; // <--- Inyectar el repositorio

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.isTokenValid(jwt, user)) {

                    
                    // Cargar las autoridades desde los roles activos del usuario en la tabla intermedia
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException ex) {
            // Token malformado, vencido, firma inválida, etc.
            // No autenticamos, pero dejamos que la cadena siga: la ruta puede ser pública
            // o la AuthorizationFilter más adelante se encarga de rechazarla con 401/403.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}