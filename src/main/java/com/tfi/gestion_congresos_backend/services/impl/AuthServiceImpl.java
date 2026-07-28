package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.AuthMapper;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.JwtService;

import lombok.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email o contraseña incorrectos"));

        if (!user.isEnabled()) {
            throw new UserDisabledException("El usuario está deshabilitado");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        LoginResponseDTO response =  authMapper.toLoginResponseDTO(user);
        String token = jwtService.generateToken(user);
        response.setToken(token);

        return response;            
    }
}