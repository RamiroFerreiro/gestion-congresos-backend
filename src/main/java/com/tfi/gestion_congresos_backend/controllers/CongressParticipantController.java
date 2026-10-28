package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfi.gestion_congresos_backend.dtos.CongressRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
        summary = "Inscribir o re-inscribir al usuario autenticado en un congreso",
        description = """
            Permite al usuario autenticado registrarse como participante en un congreso.
            
            **Reglas de negocio y comportamiento:**
            - El usuario queda inscripto automáticamente con el rol por defecto **`LISTENER` (Oyente)**.
            - El congreso debe estar habilitado (`enabled = true`).
            - La fecha actual debe estar dentro del período oficial de inscripción.
            - Se impide la doble inscripción en el mismo congreso.

            **Flujos soportados por este endpoint:**
            - **Primera inscripción:** Si el usuario no tiene historial previo en el congreso, se crea un registro activo con el rol `LISTENER`.
            - **Re-inscripción (Auto-baja previa):** Si el usuario estuvo dado de baja voluntariamente (`deletedBy == user.getCode()`), el sistema reactiva su registro previo (`active = true`), actualiza su fecha e institución, y limpia la auditoría de borrado (`deletedAt = null`, `deletedBy = null`).
            - **Bloqueo de Re-inscripción (Baja administrativa):** Si el usuario fue removido por un administrador del congreso (`deletedBy != user.getCode()`), se bloquea la solicitud retornando `403 Forbidden`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Inscripción realizada exitosamente. Devuelve mensaje de confirmación."),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: congreso deshabilitado, fuera de fecha de inscripción o usuario ya inscripto."),
        @ApiResponse(responseCode = "403", description = "Prohibido: La inscripción previa fue cancelada por un administrador del congreso y no se permite la re-inscripción directa"),
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
    
    @Operation(
        summary = "Desinscribirse voluntariamente de un congreso (Baja Lógica)",
        description = """
            Aplica la baja lógica (`active = false`) a la inscripción de un usuario objetivo dentro de un congreso específico.
            
            **Uso del endpoint (Dual):**
            - **Auto-desinscripción:** El usuario se da de baja a sí mismo pasando su propio `targetUserCode`. El servicio registrará `deletedBy = targetUserCode`, permitiéndole volver a inscribirse en el futuro.
            - **Baja por Administrador:** Un administrador del congreso da de baja a un usuario pasando el `targetUserCode` del participante. El servicio registrará `deletedBy = adminCode`, bloqueando futuras auto-reinscripciones directas por parte de ese usuario.
            
            **Detalles del Flujo:**
            1. Se recupera el usuario que ejecuta la acción desde el SecurityContext (JWT).
            2. Se busca la participación activa en el congreso mediante `targetUserCode` y `congressCode`.
            3. Se actualiza la participación estableciendo `active = false`, `deletedAt = LocalDateTime.now()` y `deletedBy = currentUser.getCode()`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Desinscripción realizada con éxito"),
        @ApiResponse(responseCode = "401", description = "No autorizado: El token JWT es inválido, ha expirado o no fue provisto"),
        @ApiResponse(responseCode = "403",description = "Prohibido: El usuario autenticado no posee los permisos de administrador sobre el congreso para dar de baja a otro usuario"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: No se encontró una inscripción activa del usuario en el congreso especificado")
    })
    @DeleteMapping("/{congressCode}/participants/{targetUserCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponseDTO> unregisterFromCongress(@PathVariable String congressCode, @PathVariable String targetUserCode) {

        MessageResponseDTO response = participantService.unregisterFromCongress(targetUserCode, congressCode);

        return ResponseEntity.ok(response);
    }
    
}
