package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.auth.ForgotPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ResetPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.AuthService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        description = "Autentica las credenciales de un usuario y devuelve sus datos de perfil junto con un token JWT de acceso."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",description = "Autenticación exitosa. Retorna token JWT y perfil del usuario"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes"),
        @ApiResponse(responseCode = "401", description = "El email o la contraseña son incorrectos"),
        @ApiResponse(responseCode = "403", description = "El usuario se encuentra deshabilitado")
    })
    @PostMapping
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request){
        
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = """
            Envía un correo electrónico con un token de recuperación si la dirección ingresada corresponde a un usuario registrado y activo en el sistema.
            
            **Nota de Seguridad:** Responde siempre con status `200 OK` e idéntico mensaje genérico para evitar la enumeración de usuarios.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitud procesada correctamente (mensaje genérico)"),
        @ApiResponse(responseCode = "400", description = "El formato de correo enviado es inválido")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }
    
    @Operation(
        summary = "Confirmar cambio de email",
        description = """
            Procesa el token de verificación de cambio de correo electrónico.
            
            - **Etapa 1 (PENDING_CURRENT_EMAIL):** Confirma la casilla actual y envía el segundo enlace a la nueva casilla.
            - **Etapa 2 (PENDING_NEW_EMAIL):** Confirma la nueva casilla, actualiza la cuenta y finaliza el proceso.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Etapa verificada correctamente (mensaje informativo según el estado)"),
        @ApiResponse(responseCode = "400", description = "El token enviado ha expirado o no es válido"),
        @ApiResponse(responseCode = "404", description = "No se encontró el token de confirmación"),
        @ApiResponse(responseCode = "409", description = "El nuevo email ya fue registrado por otro usuario antes de completar la confirmación")
    })
    @GetMapping("/confirm-email-change")
    public ResponseEntity<MessageResponseDTO> confirmEmailChange(@RequestParam String token) {

        return ResponseEntity.ok(authService.confirmEmailChange(token));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///
    
    @Operation(
        summary = "Restablecer contraseña",
        description = """
            Establece una nueva contraseña de acceso utilizando el token de recuperación recibido por correo electrónico.
            Valida la fecha de expiración, que la nueva clave no coincida con la anterior y que coincida con la confirmación.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente  "),
        @ApiResponse(responseCode = "400", description = "Las contraseñas no coinciden, la clave nueva es igual a la actual o el token ha expirado"),
        @ApiResponse(responseCode = "404", description = "El token de recuperación no existe o es inválido")
    })
    @PatchMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
