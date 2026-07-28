package com.tfi.gestion_congresos_backend.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.entities.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;



@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Value("${JWT_EXPIRATION}")
    private long jwtExpiration;

    //La clave debe tener al menos 32bytes
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user) {
        
        return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey())
            .compact();
    }

    @Override
    public String extractUsername(String token) {
        
        throw new UnsupportedOperationException("Unimplemented method 'extractUsername'");
    }

    @Override
    public boolean isTokenValid(String token, User user) {
        
        throw new UnsupportedOperationException("Unimplemented method 'isTokenValid'");
    }
    
}
