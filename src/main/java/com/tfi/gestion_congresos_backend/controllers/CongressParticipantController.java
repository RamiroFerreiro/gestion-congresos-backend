package com.tfi.gestion_congresos_backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tfi.gestion_congresos_backend.dtos.congress_participant.AdminParticipantRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantInstitutionRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
        name = "CongressParticipant",
        description = "Operaciones relacionadas con la gestión de participantes de congresos."
)
@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class CongressParticipantController {

    private final CongressParticipantService participantService;

    // ----------------------------------------------------------
    // INSCRIPCIONES
    // ----------------------------------------------------------

    @Operation(
            summary = "Inscripción propia a un congreso",
            description = """
                    Permite al usuario autenticado inscribirse en un congreso.

                    **Reglas de negocio:**
                    - El usuario debe estar autenticado.
                    - El congreso debe estar habilitado.
                    - La inscripción debe realizarse dentro del período establecido.
                    - Se asigna automáticamente el rol LISTENER.
                    - El usuario debe proporcionar su institución.
                    - No se permite una nueva inscripción si existe una participación
                      anterior, activa o inactiva, en ese congreso.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscripción realizada exitosamente."),
            @ApiResponse(responseCode = "400", description = "Congreso deshabilitado, período de inscripción inactivo o participación previa."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "404", description = "Congreso o rol LISTENER no encontrado.")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{congressCode}/participants/me")
    public ResponseEntity<MessageResponseDTO> registerToCongress(
            @PathVariable String congressCode,
            @Valid @RequestBody CongressParticipantInstitutionRequestDTO requestDTO) {

        MessageResponseDTO response =
                participantService.registerToCongress(congressCode, requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "Inscripción de un usuario por administración",
            description = """
                    Permite a un administrador de congreso o súper administrador
                    incorporar a un usuario existente como participante.

                    **Reglas de negocio:**
                    - El administrador debe tener permisos sobre el congreso.
                    - El usuario debe existir previamente en el sistema.
                    - El administrador debe indicar el rol que tendrá dentro del congreso.
                    - La institución queda sin informar hasta que el participante la complete.
                    - No se permite crear una segunda participación para el mismo usuario
                      en el mismo congreso.
                    - Las participaciones inactivas deben recuperarse mediante
                      la operación de reactivación.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participante incorporado exitosamente."),
            @ApiResponse(responseCode = "400", description = "El usuario ya tiene una participación en el congreso."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso."),
            @ApiResponse(responseCode = "404", description = "Congreso, usuario o rol no encontrado.")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{congressCode}/participants")
    public ResponseEntity<MessageResponseDTO> registerParticipantByAdmin(
            @PathVariable String congressCode,
            @Valid @RequestBody AdminParticipantRegistrationRequestDTO requestDTO) {

        MessageResponseDTO response =
                participantService.registerParticipantByAdmin(congressCode, requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ----------------------------------------------------------
    // CONSULTAS
    // ----------------------------------------------------------

    @Operation(
            summary = "Obtener participantes de un congreso",
            description = """
                    Recupera los participantes activos de un congreso específico.

                    **Parámetros:**
                    - congressCode: Código del congreso.
                    - role (opcional): Permite filtrar por un rol determinado.

                    Si no se especifica un rol, se devuelven todos los participantes activos.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participantes recuperados exitosamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso."),
            @ApiResponse(responseCode = "404", description = "Congreso no encontrado.")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{congressCode}/participants")
    public ResponseEntity<List<CongressParticipantResponseDTO>> getParticipantsByCongress(
            @PathVariable String congressCode,
            @RequestParam(required = false) RoleName role) {

        return ResponseEntity.ok(
                participantService.getParticipantsByCongress(congressCode, role)
        );
    }


    @Operation(
            summary = "Obtener un participante específico de un congreso",
            description = """
                    Recupera los datos de una participación activa utilizando el código
                    del usuario y el código del congreso.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante recuperado exitosamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso."),
            @ApiResponse(responseCode = "404", description = "No se encontró una participación activa.")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{congressCode}/participants/{userCode}")
    public ResponseEntity<CongressParticipantResponseDTO> getParticipantByUserCode(
            @PathVariable String congressCode,
            @PathVariable String userCode) {

        return ResponseEntity.ok(
                participantService.getParticipantByUserCode(congressCode, userCode)
        );
    }

    @Operation(
            summary = "Obtener la participación del usuario autenticado en un congreso",
            description = """
                    Recupera los datos de la participación activa del usuario autenticado
                    en el congreso indicado.

                    La consulta utiliza automáticamente la identidad del usuario autenticado,
                    por lo que no es necesario proporcionar su código.

                    **Permisos:**
                    - Usuarios autenticados.

                    **Consideraciones:**
                    - Solo se recuperan participaciones activas.
                    - El usuario únicamente puede consultar su propia participación.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participación recuperada exitosamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "404", description = "No se encontró el congreso o una participación activa del usuario en el mismo.")
    })
    @GetMapping("/{congressCode}/participants/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CongressParticipantResponseDTO> getAuthenticatedParticipantInCongress(
            @PathVariable String congressCode) {

        return ResponseEntity.ok(
                participantService.getAuthenticatedParticipantInCongress(congressCode)
        );
    }

    // ----------------------------------------------------------
    // BAJAS Y MODIFICACIONES
    // ----------------------------------------------------------

    @Operation(
            summary = "Dar de baja una participación",
            description = """
                    Realiza una baja lógica de la participación de un usuario en un congreso.

                    **Reglas de negocio:**
                    - El usuario puede darse de baja a sí mismo.
                    - Un administrador puede dar de baja a otro participante de su congreso.
                    - Un súper administrador también puede realizar esta operación.
                    - Se registran la fecha de baja y el código del usuario que la realizó.
                    - No se permite dar de baja al último administrador activo del congreso.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Baja lógica realizada exitosamente."),
            @ApiResponse(responseCode = "400", description = "No se puede dar de baja al último administrador activo."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No posee permisos para dar de baja a otro participante."),
            @ApiResponse(responseCode = "404", description = "No se encontró una participación activa.")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{congressCode}/participants/{userCode}")
    public ResponseEntity<MessageResponseDTO> unregisterFromCongress(
            @PathVariable String congressCode,
            @PathVariable String userCode) {

        MessageResponseDTO response =
                participantService.unregisterFromCongress(userCode, congressCode);

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Modificar el rol de un participante",
            description = """
                    Modifica el rol de un participante dentro de un congreso.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.

                    **Reglas de negocio:**
                    - El participante debe tener una inscripción activa.
                    - Se permite asignar el rol ADMINISTRATOR a otro participante.
                    - No se permite quitar el rol ADMINISTRATOR al último administrador activo.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rol actualizado exitosamente."),
            @ApiResponse(responseCode = "400", description = "Rol inválido o se intenta quitar el rol al último administrador."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso."),
            @ApiResponse(responseCode = "404", description = "Participante o rol no encontrado.")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{congressCode}/participants/{userCode}/role")
    public ResponseEntity<Void> updateParticipantRole(
            @PathVariable String congressCode,
            @PathVariable String userCode,
            @RequestBody RoleName newRole) {

        participantService.updateParticipantRole(congressCode, userCode, newRole);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Actualizar la institución del participante autenticado",
            description = """
                    Permite al usuario autenticado actualizar la institución
                    correspondiente a su participación en un congreso.

                    **Reglas de negocio:**
                    - El usuario debe estar autenticado.
                    - Debe tener una participación activa en el congreso.
                    - Solo puede modificar su propia institución.
                    - Puede actualizarla aunque ya tenga un valor informado.
                    - La institución no puede estar vacía.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institución actualizada exitosamente."),
            @ApiResponse(responseCode = "400", description = "La institución no es válida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "404", description = "Congreso o participación activa no encontrada.")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{congressCode}/participants/me/institution")
    public ResponseEntity<MessageResponseDTO> updateAuthenticatedParticipantInstitution(
            @PathVariable String congressCode,
            @Valid @RequestBody CongressParticipantInstitutionRequestDTO requestDTO) {

        MessageResponseDTO response =
                participantService.updateAuthenticatedParticipantInstitution(congressCode, requestDTO);

        return ResponseEntity.ok(response);
    }
}