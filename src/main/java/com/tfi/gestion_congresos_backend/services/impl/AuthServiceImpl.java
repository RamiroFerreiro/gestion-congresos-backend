package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.auth.ForgotPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ResetPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.PasswordResetToken;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.AuthMapper;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

import jakarta.transaction.Transactional;

import com.tfi.gestion_congresos_backend.repository.PasswordResetTokenRepository;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.JwtService;

import lombok.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final EmailService emailService;
    
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

    @Override
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        
        ///Traer el usuario por mail
        User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);

        PasswordResetToken passwordResetToken;

        if (existingToken.isPresent()) {
            passwordResetToken = existingToken.get();
        } else {
            passwordResetToken = new PasswordResetToken();
        }

        passwordResetToken.setUser(user);
        passwordResetToken.setToken(UUID.randomUUID().toString());
        passwordResetToken.setExpirationDate(DateUtils.now().plusMinutes(30));

        passwordResetTokenRepository.save(passwordResetToken);

        emailService.sendPasswordResetEmail(user, passwordResetToken.getToken());

        return MessageResponseDTO.builder()
            .message("Si el mail existe, el link de recuperación de contraseña a sido enviado.")
            .build();
    }

    @Transactional
    @Override
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new ResourceNotFoundException("El token de recuperación no es válido."));

        User user = passwordResetToken.getUser();

        
        validateTokenExpiration(passwordResetToken.getExpirationDate());
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validateNewPassword(user.getPassword(), request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(passwordResetToken);

        return MessageResponseDTO.builder().message("Contraseña recuperada correctamente.").build();
    }

    ///PRIVADOS
    private void validatePasswordConfirmation(String newPassword, String confirmPassword){
        if (!newPassword.equals(confirmPassword)) {

            throw new ArgumentNotValidException("Las contraseñas no coinciden");
        }
    }

    private void validateNewPassword(String encodedCurrentPassword, String newPassword){
        if (passwordEncoder.matches(newPassword, encodedCurrentPassword)) {

            throw new ArgumentNotValidException("La contraseña nueva debe ser diferente a la actual");
        }
    }

    private void validateTokenExpiration(LocalDateTime expirationDate){

        if(expirationDate.isBefore(DateUtils.now())){

            throw new ArgumentNotValidException("El token de recuperación ha expirado.");
        }
    }


}