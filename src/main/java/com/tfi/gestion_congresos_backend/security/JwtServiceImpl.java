package com.tfi.gestion_congresos_backend.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.function.Function;



@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Value("${JWT_EXPIRATION}")
    private long jwtExpiration;

    ///MÉTODOS PRIVADOS
    
    //La clave debe tener al menos 32bytes
    //Retorna un Objeto con la clave secreta para firmar el token
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    //retorna todos los claims del token
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //retorna el claim enviado por funcion
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver){

        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //retorna fecha de expiración del token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //verifica si el token está vencido
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    ///MÉTODOS PÚBLICOS
    //Genera token
    @Override
    public String generateToken(User user) {
        
        return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey())
            .compact();
    }
    //extrae usuario
    @Override
    public String extractUsername(String token) {
        
        return extractClaim(token, Claims::getSubject);
    }

    //valida token si corresponde a usuario y fecha de vencimiento
    @Override
    public boolean isTokenValid(String token, User user) {
        
        String username = extractUsername(token);

        return username.equals(user.getEmail())&& !isTokenExpired(token);
    }
    
}
