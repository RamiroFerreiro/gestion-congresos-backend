package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.auth.ForgotPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ResetPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    ///----------------------------------------------------------POST----------------------------------------------------------///

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario mediante su email y contraseña y genera un token JWT."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesión exitoso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "El email o la contraseña son incorrectos"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario se encuentra deshabilitado"
            )
    })
    @SecurityRequirement(name = "")
    @PostMapping
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request){
        
        return ResponseEntity.ok(authService.login(request));
    }
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Inicia el proceso de recuperación de contraseña para un usuario mediante su dirección de email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Solicitud de recuperación procesada correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El email proporcionado no tiene un formato válido"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario asociado al email proporcionado"
            )
    })
    @SecurityRequirement(name = "")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///
    
    @Operation(
            summary = "Restablecer contraseña",
            description = "Permite establecer una nueva contraseña utilizando un token de recuperación válido."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña restablecida correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "La solicitud no es válida, el token ha expirado, las contraseñas no coinciden o la nueva contraseña coincide con la actual"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el token de recuperación"
            )
    })
    @SecurityRequirement(name = "")
    @PatchMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
