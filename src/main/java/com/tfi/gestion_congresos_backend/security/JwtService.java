package com.tfi.gestion_congresos_backend.security;

import com.tfi.gestion_congresos_backend.entities.User;


public interface JwtService {
    
    String generateToken(User user);

    String extractUsername(String token);

    boolean isTokenValid(String token, User user);
}
