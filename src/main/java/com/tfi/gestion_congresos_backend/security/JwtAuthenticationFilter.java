package com.tfi.gestion_congresos_backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.repository.UserRepository;

import io.jsonwebtoken.lang.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtService jwtService;

    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        //leer el Authorization Header
        final String authHeader = request.getHeader("Authorization");

        //verificar si existe token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        //extraer el token
        String jwt = authHeader.substring(7);

        ///extraer el mail
        String userEmail = jwtService.extractUsername(jwt);

        //Verificar si ya hay un usuario autenticado y si el mail es válido
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            //buscar al usuario en la bd con su rol asociado
            User user = userRepository.findByEmailWithRole(userEmail).orElse(null);

            //verificar que el usuario exista, el token sea válido(corresponda al usuario y no haya expirado)
            if (user != null && jwtService.isTokenValid(jwt, user)) {

                ///creo una lista con las autoridades del usuario
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name())
                );
            
                System.out.println(authorities);
                //Crear el objeto de autenticación que Spring Security almacenará para esta petición
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                //Asociar información adicional de la petición HTTP
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //Setear usuario autenticado
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
    
}
