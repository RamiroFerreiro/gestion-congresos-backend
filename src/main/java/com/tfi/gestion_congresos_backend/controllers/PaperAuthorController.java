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
                Recupera la lista completa y ordenada de todos los autores (tanto ACCEPTED como PENDING) vinculados a un trabajo específico.
                
                **Políticas de Control de Acceso:**
                - **Súper Admin / Admin de Congreso:** Acceso total.
                - **Co-autor del trabajo:** Puede ver la lista de su propio trabajo.
                - **Otros roles:** Denegado con `403 Forbidden`.
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

    ///----------------------------------------------------------POSTS----------------------------------------------------------///

    @Operation(
            summary = "Solicitar unirse como co-autor a un trabajo",
            description = """
                Permite a un usuario solicitar unirse a un trabajo existente. 
                La relación se crea en estado **PENDING** y debe ser aceptada por el Autor Principal o un Administrador.
                
                **Reglas de negocio:**
                - El usuario autenticado debe ser quien realiza la solicitud (no se puede solicitar en nombre de otro).
                - El usuario no debe tener rol de EVALUADOR en el congreso.
                - El trabajo debe estar en estado `NOT_SUBMITTED`.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada exitosamente (Estado PENDING)"),
            @ApiResponse(responseCode = "400", description = "El trabajo no está en estado de edición o el usuario es Evaluador"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "El código de usuario no coincide con el usuario autenticado"),
            @ApiResponse(responseCode = "404", description = "El trabajo o el participante no existen"),
            @ApiResponse(responseCode = "409", description = "El usuario ya posee una relación con el trabajo")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/requests/{userCode}")
    public ResponseEntity<List<PaperAuthorResponseDTO>> requestToJoinPaper(
            @PathVariable String paperCode, 
            @PathVariable String userCode) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperAuthorService.requestToJoinPaper(paperCode, userCode));
    }

    @Operation(
            summary = "Agregar directamente un autor al trabajo (Solo Admin)",
            description = """
                Permite a un Administrador de congreso o Súper Admin insertar directamente a un usuario como autor **ACCEPTED**, saltándose el flujo de solicitud.
                
                **Reglas de negocio:**
                - Si el usuario era LISTENER, se le asciende automáticamente a EXPOSITOR.
                - Respeta el límite máximo de autores definido por el congreso.
                - Exclusivo para administradores.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Autor agregado correctamente al trabajo"),
            @ApiResponse(responseCode = "400", description = "Se supera el límite máximo de autores o el usuario es Evaluador"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (Requiere rol de Administrador)"),
            @ApiResponse(responseCode = "404", description = "El trabajo o el usuario no existen"),
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
                Permite actualizar en lote el orden de aparición (`authorOrder`), el rol de presentador (`isPresentator`) y transferir la autoría principal (`isMainAuthor`).
                
                **Políticas y Reglas de Negocio:**
                - Solo el **Autor Principal**, Administrador o Súper Admin pueden ejecutar esta acción.
                - La transferencia de la bandera `isMainAuthor` hacia otro autor está **restringida exclusivamente a Administradores/Súper Admins**.
                - Debe existir siempre exactamente un autor con `isMainAuthor = true`.
                - Solo aplica para autores en estado `ACCEPTED`.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autores actualizados y reordenados correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. más de un Main Author o intentos de modificar solicitudes pendientes)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (intentó transferir autoría sin ser Admin, o no posee permisos sobre el trabajo)"),
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
            summary = "Aceptar solicitud de co-autoría",
            description = """
                Cambia el estado de una solicitud de PENDING a ACCEPTED.
                Solo puede ser ejecutado por el Autor Principal del trabajo o un Administrador del congreso.
                
                - Si el participante aceptado es LISTENER, se le asciende a EXPOSITOR automáticamente.
                - Valida el cupo máximo de autores del congreso.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud aceptada exitosamente"),
            @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED o el usuario ya estaba aceptado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (Requiere ser Autor Principal o Administrador)"),
            @ApiResponse(responseCode = "404", description = "No existe una solicitud pendiente para el usuario indicado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/requests/{userCode}/accept")
    public ResponseEntity<List<PaperAuthorResponseDTO>> acceptAuthorRequest(
            @PathVariable String paperCode, 
            @PathVariable String userCode) {
        return ResponseEntity.ok(paperAuthorService.acceptAuthorRequest(paperCode, userCode));
    }

    @Operation(
            summary = "Rechazar solicitud de co-autoría",
            description = """
                Rechaza una solicitud en estado PENDING, eliminándola de la base de datos sin dejar historial.
                Solo puede ser ejecutado por el Autor Principal del trabajo o un Administrador del congreso.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada y eliminada correctamente"),
            @ApiResponse(responseCode = "400", description = "El trabajo no está en estado NOT_SUBMITTED o el usuario no está PENDING"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (Requiere ser Autor Principal o Administrador)"),
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
                - No se puede eliminar al Autor Principal (isMainAuthor = true). Si se requiere, se debe transferir la autoría mediante Bulk Update primero.
                - No utilizar este endpoint para solicitudes PENDING (utilizar el endpoint de rechazo).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autor eliminado y reordenamiento aplicado correctamente"),
            @ApiResponse(responseCode = "400", description = "Intento de eliminar al Autor Principal o a una solicitud PENDING"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (Requiere ser Autor Principal o Administrador)"),
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