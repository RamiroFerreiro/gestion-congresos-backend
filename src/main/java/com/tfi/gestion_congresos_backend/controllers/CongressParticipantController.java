package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfi.gestion_congresos_backend.dtos.CongressRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@Tag(name = "CongressParticipant", description = "Operaciones relacionadas con la gestión usuarios de congresos")
@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class CongressParticipantController {

    private final CongressParticipantService participantService;

    @Operation(
        summary = "Inscribirse a un congreso",
        description = """
            Permite al usuario autenticado registrarse como participante en un congreso.
            
            **Reglas de negocio y comportamiento:**
            - El usuario queda inscripto automáticamente con el rol por defecto **`LISTENER` (Oyente)**.
            - El congreso debe estar habilitado (`enabled = true`).
            - La fecha actual debe estar dentro del período oficial de inscripción.
            - Se impide la doble inscripción en el mismo congreso.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Inscripción realizada exitosamente. Devuelve mensaje de confirmación."),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: congreso deshabilitado, fuera de fecha de inscripción o usuario ya inscripto."),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado o token JWT inválido."),
        @ApiResponse(responseCode = "404", description = "Congreso no encontrado con el código proporcionado.")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{congressCode}/register")
    public ResponseEntity<MessageResponseDTO> registerToCongress(@PathVariable String congressCode,
            @Valid @RequestBody CongressRegistrationRequestDTO requestDTO) {

        MessageResponseDTO response = participantService.registerToCongress(congressCode, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } 
    
}
