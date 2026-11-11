package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.services.PaperAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers/{paperCode}/authors")
@RequiredArgsConstructor
@Tag(name = "Paper Authors", description = "Operaciones dedicadas a la gestión del ciclo de vida de los autores de un trabajo (Paper)")
public class PaperAuthorController {

    private final PaperAuthorService paperAuthorService;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Operation(
            summary = "Obtener el listado de autores de un trabajo",
            description = """
                Recupera la lista ordenada de autores que fueron aceptados (ACCEPTED)
			    para un trabajo específico.
			
			    Las solicitudes PENDING se consultan mediante el endpoint
			    específico de solicitudes pendientes.
			
			    **Políticas de Control de Acceso:**
			    - Súper Admin / Admin de Congreso: acceso.
			    - Usuarios vinculados al trabajo como autores: pueden consultar sus autores.
			    - Otros usuarios: acceso denegado.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de autores recuperado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ser autor del trabajo o administrador)"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<PaperAuthorResponseDTO>> getPaperAuthors(@PathVariable String paperCode) {
        return ResponseEntity.ok(paperAuthorService.getPaperAuthors(paperCode));
    }
    
    @Operation(
            summary = "Obtener solicitudes pendientes de incorporación a un trabajo",
            description = """
                    Recupera las solicitudes de incorporación que se encuentran en estado PENDING
                    para un trabajo específico.

                    **Permisos:**
                    - Autor principal del trabajo.
                    - Administradores del congreso.
                    - Súper administradores.

                    Requiere que el usuario autenticado tenga su institución definida,
                    salvo que sea superadministrador.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitudes pendientes recuperadas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para consultar las solicitudes"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/requests/pending")
    public ResponseEntity<List<PaperAuthorResponseDTO>> getPendingAuthorRequests(
            @PathVariable String paperCode) {

        return ResponseEntity.ok(
                paperAuthorService.getPendingAuthorRequests(paperCode)
        );
    }

    ///----------------------------------------------------------POSTS----------------------------------------------------------///

    @Operation(
            summary = "Solicitar unirse como co-autor a un trabajo",
            description = """
                Permite a un usuario solicitar incorporarse a un trabajo existente.
			    La relación se crea en estado PENDING y debe ser aceptada por el
			    Autor Principal, un Administrador del congreso o un Súper Administrador.
			
			    **Reglas de negocio:**
			    - El usuario autenticado debe ser quien realiza la solicitud.
			    - Solo pueden solicitar incorporarse usuarios con rol LISTENER o EXPOSITOR.
			    - No se permiten solicitudes de Administradores, Evaluadores ni Súper Administradores.
			    - El usuario debe tener participación activa en el congreso.
			    - El trabajo debe estar en estado NOT_SUBMITTED.
                """
    )
    @ApiResponses({
	        @ApiResponse(responseCode = "201", description = "Solicitud creada exitosamente (Estado PENDING)"),
	        @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED"),
	        @ApiResponse(responseCode = "401", description = "No autenticado"),
	        @ApiResponse(responseCode = "403", description = "El código no coincide con el usuario autenticado, el usuario no tiene un rol permitido o es superadministrador"),
	        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo o una participación activa del usuario en el congreso"),
	        @ApiResponse(responseCode = "409", description = "El usuario ya posee una relación con el trabajo")
	})
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/requests/{userCode}")
    public ResponseEntity<PaperAuthorResponseDTO> requestToJoinPaper(
            @PathVariable String paperCode,
            @PathVariable String userCode) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperAuthorService.requestToJoinPaper(paperCode, userCode));
    }

    @Operation(
            summary = "Agregar directamente un autor al trabajo (Admin o Súper Admin)",
            description = """
                Permite a un Administrador de congreso o Súper Admin insertar directamente a un usuario como autor **ACCEPTED**, saltándose el flujo de solicitud.
                
                **Reglas de negocio:**
                - El usuario incorporado debe tener participación activa y rol LISTENER o EXPOSITOR.
                - Si el usuario era LISTENER, se le asciende automáticamente a EXPOSITOR.
                - Respeta el límite máximo de autores definido por el congreso.
                - Exclusivo para administradores del congreso y superadministradores..
                """
    )
    @ApiResponses({
	        @ApiResponse(responseCode = "201", description = "Autor agregado correctamente al trabajo"),
	        @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED o se supera el límite máximo de autores"),
	        @ApiResponse(responseCode = "401", description = "No autenticado"),
	        @ApiResponse(responseCode = "403", description = "No tiene permisos de administración o el usuario incorporado no cumple los requisitos de rol"),
	        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo, el usuario o una participación activa en el congreso"),
	        @ApiResponse(responseCode = "409", description = "El usuario ya posee una relación con el trabajo")
	})
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{userCode}/admin")
    public ResponseEntity<List<PaperAuthorResponseDTO>> addAuthorDirectly(
            @PathVariable String paperCode, 
            @PathVariable String userCode) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperAuthorService.addAuthorDirectly(paperCode, userCode));
    }

    ///----------------------------------------------------------PUT / BULK UPDATE----------------------------------------------------------///

    @Operation(
            summary = "Actualización masiva de atributos de co-autores (Bulk Update)",
            description = """
                Permite actualizar en lote el orden de aparición (authorOrder) y el indicador de presentador (isPresentator) de los autores aceptados.
			
			    **Políticas y reglas de negocio:**
			    - El trabajo debe estar en estado NOT_SUBMITTED.
			    - Solo el Autor Principal, Administradores o Súper Administradores pueden ejecutar esta acción.
			    - Deben enviarse todos los autores ACCEPTED del trabajo.
			    - Los autores no pueden repetirse.
			    - Los órdenes deben ser únicos, positivos y consecutivos desde 1.
			    - No permite modificar la autoría principal (isMainAuthor).
			    - No permite modificar solicitudes PENDING.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autores actualizados y reordenados correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Uno o más usuarios de la lista no pertenecen al trabajo")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping
    public ResponseEntity<List<PaperAuthorResponseDTO>> updatePaperAuthors(
            @PathVariable String paperCode, 
            @Valid @RequestBody List<PaperAuthorUpdateRequestDTO> updates) {
        return ResponseEntity.ok(paperAuthorService.updatePaperAuthors(paperCode, updates));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///

    @Operation(
            summary = "Transferir la autoría principal de un trabajo",
            description = """
                    Transfiere la responsabilidad del trabajo a otro de sus autores aceptados.

                    **Permisos:**
                    - Autor principal actual.
                    - Administradores del congreso.
                    - Súper administradores.

                    **Reglas de negocio:**
                    - El trabajo debe encontrarse en estado NOT_SUBMITTED.
                    - El nuevo responsable debe ser un autor ACCEPTED.
                    - El autor principal anterior permanece como autor aceptado.
                    - No se modifica el orden de autoría ni el indicador de presentador.
                    - Debe existir exactamente un autor principal después de la operación.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autoría principal transferida correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado del trabajo inválido o nuevo responsable no aceptado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para transferir la autoría principal"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo o el autor indicado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/main-author/{newMainAuthorCode}/transfer")
    public ResponseEntity<List<PaperAuthorResponseDTO>> transferMainAuthor(
            @PathVariable String paperCode,
            @PathVariable String newMainAuthorCode
    ) {
        return ResponseEntity.ok(
                paperAuthorService.transferMainAuthor(paperCode, newMainAuthorCode)
        );
    }
    
    @Operation(
            summary = "Aceptar solicitud de coautoría",
            description = """
                    Cambia el estado de una solicitud de PENDING a ACCEPTED.

                    **Permisos:**
                    - Autor principal del trabajo.
                    - Administradores del congreso.
                    - Súper administradores.

                    **Reglas de negocio:**
                    - El trabajo debe estar en estado NOT_SUBMITTED.
                    - El solicitante debe conservar una participación activa en el congreso.
                    - El solicitante debe tener rol LISTENER o EXPOSITOR al momento de la aceptación.
                    - Si el solicitante tiene rol LISTENER, se lo promueve automáticamente a EXPOSITOR.
                    - Se valida el cupo máximo de autores permitido por el congreso.
                    - La aceptación asigna automáticamente el siguiente orden de autoría disponible.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud aceptada exitosamente"),
            @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED, la solicitud no está pendiente o se superó el máximo de autores"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para aceptar solicitudes o el solicitante ya no cumple los requisitos de rol"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo, la solicitud pendiente o una participación activa del solicitante en el congreso")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/requests/{userCode}/accept")
    public ResponseEntity<List<PaperAuthorResponseDTO>> acceptAuthorRequest(
            @PathVariable String paperCode,
            @PathVariable String userCode) {
        return ResponseEntity.ok(
                paperAuthorService.acceptAuthorRequest(paperCode, userCode)
        );
    }

    @Operation(
            summary = "Rechazar solicitud de co-autoría",
            description = """
                Rechaza una solicitud en estado PENDING, eliminándola de la base de datos sin conservar historial.

		        **Permisos:**
		        - Autor principal del trabajo.
		        - Administradores del congreso.
		        - Súper administradores.
		
		        El solicitante podrá volver a solicitar incorporarse al trabajo
		        mientras este permanezca en estado NOT_SUBMITTED.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada y eliminada correctamente"),
            @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED o el usuario no está PENDING"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para rechazar solicitudes"),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud pendiente para el usuario indicado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/requests/{userCode}/reject")
    public ResponseEntity<List<PaperAuthorResponseDTO>> rejectAuthorRequest(
            @PathVariable String paperCode, 
            @PathVariable String userCode) {
        return ResponseEntity.ok(paperAuthorService.rejectAuthorRequest(paperCode, userCode));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///

    @Operation(
            summary = "Eliminar un autor de un trabajo",
            description = """
                Elimina permanentemente la relación de un autor ACEPTADO con el trabajo y compacta el orden de aparición del resto de los autores.
                
                **Reglas de negocio:**
                - No se puede eliminar al Autor Principal (isMainAuthor = true). Si se requiere transferir la autoría principal, debe utilizarse el endpoint específico de transferencia de autoría.
                - No utilizar este endpoint para solicitudes PENDING (utilizar el endpoint de rechazo).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor eliminado y reordenamiento aplicado correctamente"),
            @ApiResponse(responseCode = "400", description = "Intento de eliminar al Autor Principal o a una solicitud PENDING"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (Requiere ser Autor Principal, Administrador o Súper Administrador)"),
            @ApiResponse(responseCode = "404", description = "El usuario no tiene relación con este trabajo")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{userCode}")
    public ResponseEntity<List<PaperAuthorResponseDTO>> removeAuthorFromPaper(
            @PathVariable String paperCode, 
            @PathVariable String userCode) {
        return ResponseEntity.ok(paperAuthorService.removeAuthorFromPaper(paperCode, userCode));
    }
}