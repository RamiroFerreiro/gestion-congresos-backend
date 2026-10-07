package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ForgotPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ResetPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.EmailChangeToken;
import com.tfi.gestion_congresos_backend.entities.PasswordResetToken;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.EmailChangeStatus;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.AuthMapper;
import com.tfi.gestion_congresos_backend.mapper.UserMapper;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

import jakarta.transaction.Transactional;

import com.tfi.gestion_congresos_backend.repository.EmailChangeTokenRepository;
import com.tfi.gestion_congresos_backend.repository.PasswordResetTokenRepository;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.JwtService;

import lombok.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final EmailService emailService;

    ///----------------------------------------------------------LOGIN ----------------------------------------------------------///
    
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        
        //1. Buscar al usuario por email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email o contraseña incorrectos"));

        //2. Validar contraseña PRIMERO (evita confirmarle a un atacante que el email existe si la cuenta está deshabilitada)
        if (!user.isEnabled()) {
            throw new UserDisabledException("El usuario está deshabilitado");
        }

        //3. Validar si la cuenta está habilitada RECIÉN después de confirmar la contraseña correcta
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        //4. Mapear la respuesta, generar el JWT y retornarlo
        LoginResponseDTO response =  authMapper.toLoginResponseDTO(user);
        String token = jwtService.generateToken(user);
        response.setToken(token);

        return response;            
    }

    ///----------------------------------------------------------CHANGE PASSWORD----------------------------------------------------------///
    
    @Override
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        
        //1. Buscar al usuario de forma opcional sin lanzar excepción si no existe
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        //2. Si el usuario existe, procesar la generación del token y el envío de correo
        if (userOptional.isPresent()) {

            User user = userOptional.get();

            // Si la cuenta está deshabilitada, no enviamos correo 
            if (user.isEnabled()) {
                
                // Buscar si ya tiene un token previo o instanciar uno nuevo
                PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUser(user)
                        .orElseGet(PasswordResetToken::new);

                // Cargar o actualizar los atributos del token
                passwordResetToken.setUser(user);
                passwordResetToken.setToken(UUID.randomUUID().toString());
                passwordResetToken.setExpirationDate(DateUtils.now().plusMinutes(30));

                // Persistir en base de datos
                passwordResetTokenRepository.save(passwordResetToken);

                // Enviar el correo de forma asíncrona
                emailService.sendPasswordResetEmail(user, passwordResetToken.getToken());
            }
        }

        // 3. Responder SIEMPRE con el mismo mensaje genérico y status 200 OK
        return MessageResponseDTO.builder()
                .message("Si el mail existe, el link de recuperación de contraseña ha sido enviado.")
                .build();
    }

    ///----------------------------------------------------------RESET PASSWORD----------------------------------------------------------///
     
    @Transactional
    @Override
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {


        //1. Buscar el token, si no existe lanza excepción
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new ResourceNotFoundException("El token de recuperación no es válido."));

        //2. Si existe, traemos el usuario atado al token
        User user = passwordResetToken.getUser();

        //3. Validar que el token recibido no haya experido, que las contraseñas coincidas y que la nueva no sea identica a la actual
        validateTokenExpiration(passwordResetToken.getExpirationDate());
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());
        validateNewPassword(user.getPassword(), request.getNewPassword());

        //4. Setear, encriptar y guardar la nueva contraseña
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        //5. Borrar el token de recuperación
        passwordResetTokenRepository.delete(passwordResetToken);

        //6. Retonar mensaje
        return MessageResponseDTO.builder().message("Contraseña recuperada correctamente.").build();
    }
     ///----------------------------------------------------------CONFIRM MAIL----------------------------------------------------------///
     
    @Transactional
    @Override
    public MessageResponseDTO confirmEmailChange(String token) {

        //1. Recuperar el registro de confirmación a partir del token único recibido en el enlace
        EmailChangeToken emailChangeToken = emailChangeTokenRepository.findByToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("El token de cambio de email no es válido."));

        //2. Validar que la fecha actual no haya superado la hora de expiración registrada en el token
        validateTokenExpiration(emailChangeToken.getExpirationDate());

        //3. Declarar la variable que almacenará el mensaje informativo de la etapa
        String message = "";

        //4. PRIMERA ETAPA (Confirmación desde casilla actual)
        if (emailChangeToken.getStatus() == EmailChangeStatus.PENDING_CURRENT_EMAIL) {

            message = confirmNewEmail(emailChangeToken);
            
            //5. SEGUNDA ETAPA (Confirmación desde la nueva casilla)
        }else if (emailChangeToken.getStatus() == EmailChangeStatus.PENDING_NEW_EMAIL) {
                
            message = saveNewEmail(emailChangeToken);
        }

        //6. Construir y retornar la respuesta con el mensaje correspondiente a la etapa procesada
        return MessageResponseDTO.builder().message(message).build();
    }

    ///---------------------------------------------------------- PRIVADOS ----------------------------------------------------------///
    
    private void validatePasswordConfirmation(String newPassword, String confirmPassword){
        if (newPassword == null || !newPassword.equals(confirmPassword)) {

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

    private String confirmNewEmail(EmailChangeToken emailChangeToken){

        emailChangeToken.setToken(UUID.randomUUID().toString());

        emailChangeToken.setExpirationDate(DateUtils.now().plusMinutes(30));

        emailChangeToken.setStatus(EmailChangeStatus.PENDING_NEW_EMAIL);

        // Persistir explícitamente el nuevo token y estado para evitar inconsistencias
        emailChangeTokenRepository.save(emailChangeToken);

        emailService.sendNewEmailChangeVerificationEmail(
                    emailChangeToken.getNewEmail(),
                    emailChangeToken.getUser(),
                    emailChangeToken.getToken()
        );

        String message = "El email actual fue verificado. " +
                        "Se ha enviado un enlace de confirmación " +
                        "al nuevo email.";
        return message;
    }

    private String saveNewEmail(EmailChangeToken emailChangeToken){


        //1. Validar nuevamente que el email siga disponible antes de aplicar el cambio
        if (userRepository.existsByEmail(emailChangeToken.getNewEmail())) {
            throw new ResourceAlreadyExistsException("El nuevo email ya se encuentra registrado por otro usuario.");
        }

        //2. Obtener la entidad del usuario asociada a la solicitud de cambio y a   signar la nueva casilla de correo
        User user = emailChangeToken.getUser();
        user.setEmail(emailChangeToken.getNewEmail());

        //3. Persistir los cambios en la tabla de usuarios
        userRepository.save(user);

        //4. Eliminar el token usado para no dejar basura en BD
        emailChangeTokenRepository.delete(emailChangeToken);

        String message = "El email fue cambiado correctamente.";
        
        // 6. Retornar el mensaje de éxito del proceso completo
        return message;
    }

}