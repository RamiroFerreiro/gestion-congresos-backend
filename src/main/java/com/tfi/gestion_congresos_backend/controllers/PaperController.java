package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperPendingReassignmentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerWorkloadResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperService;

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
import java.util.Set;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Operaciones relacionadas con la gestión del ciclo de vida y metadatos de los trabajos (Papers)")
public class PaperController {

    private final PaperService paperService;

    ///----------------------------------------------------------GETS----------------------------------------------------------///
    
    @Operation(
            summary = "Obtener trabajos asignados a un evaluador",
            description = """
                    Obtiene los trabajos asignados a un evaluador dentro de un congreso.
                    El evaluador puede consultar sus propios trabajos y los administradores
                    del congreso o superadministradores pueden consultar los trabajos
                    de cualquier evaluador activo del congreso.
                    Requiere que el usuario autenticado tenga su institución definida.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trabajos obtenidos correctamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos para consultar los trabajos o tiene pendiente completar su institución"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el congreso o un evaluador activo con el código especificado"
            )
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/congresses/{congressCode}/assigned-to/{reviewerCode}")
    public ResponseEntity<List<PaperSummaryResponseDTO>> getAssignedPapers(
    		@PathVariable String congressCode,
    		@PathVariable String reviewerCode) {

        return ResponseEntity.ok(
                paperService.getAssignedPapers(congressCode, reviewerCode)
        );
    }
    
    @Operation(
            summary = "Obtener evaluadores disponibles y su carga de trabajo",
            description = """
                    Obtiene los evaluadores disponibles de un congreso y la cantidad
                    de trabajos pendientes de evaluación que tiene asignados cada uno.

                    Se consideran disponibles los usuarios cuya cuenta global está
                    habilitada, tienen una participación activa y poseen el rol
                    EVALUATOR en el congreso indicado.

                    Los evaluadores sin trabajos asignados también se incluyen,
                    con una carga de trabajo igual a cero.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.

                    Requiere que el usuario autenticado tenga su institución definida,
                    salvo que sea superadministrador.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluadores recuperados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administración o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el congreso")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/congresses/{congressCode}/reviewers/available")
    public ResponseEntity<List<ReviewerWorkloadResponseDTO>> getAvailableReviewersWithWorkload(
            @PathVariable String congressCode) {

        return ResponseEntity.ok(
                paperService.getAvailableReviewersWithWorkload(congressCode)
        );
    }
    
    @Operation(
            summary = "Obtener trabajos pendientes de reasignación",
            description = """
                    Obtiene los trabajos cuyo evaluador asignado ya no cumple
                    las condiciones para evaluar en el congreso.

                    Se incluyen trabajos en estado NOT_SUBMITTED,
                    UNDER_EVALUATION o NEEDS_REVISION.

                    Se excluyen los trabajos que tengan una evaluación
                    pendiente de liberación para su versión actual.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.

                    Requiere que el usuario autenticado tenga su institución
                    definida, salvo que sea superadministrador.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajos pendientes recuperados correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administración o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el congreso")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/congresses/{congressCode}/pending-reassignment")
    public ResponseEntity<List<PaperPendingReassignmentResponseDTO>> getPapersPendingReassignment(
            @PathVariable String congressCode) {

        return ResponseEntity.ok(
                paperService.getPapersPendingReassignment(congressCode)
        );
    }

    @Operation(
            summary = "Obtener todos los trabajos de un congreso",
            description = "Obtiene la lista resumida de todos los trabajos de un congreso, identificado por su código."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró un congreso con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<PaperSummaryResponseDTO>> getPapersByCongress(@RequestParam String congressCode) {
        return ResponseEntity.ok(paperService.getPapersByCongressCode(congressCode));
    }

    @Operation(
            summary = "Obtener el detalle de un trabajo por código",
            description = "Obtiene la información completa de un Paper a partir de su código de trackeo, incluyendo autores y evaluadores si corresponde."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajo obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró un Paper con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{paperCode}")
    public ResponseEntity<PaperDetailResponseDTO> getPaperByCode(@PathVariable String paperCode) {
        return ResponseEntity.ok(paperService.getPaperByCode(paperCode));
    }

    ///----------------------------------------------------------POSTS----------------------------------------------------------///
    
    @Operation(
            summary = "Crear un nuevo trabajo (Paper)",
            description = """
                Crea un nuevo Paper en estado NOT_SUBMITTED. El código de trackeo se genera automáticamente.
                El resumen admite HTML restringido y sanitizado: p, strong, b, em, i, u,
                sub, sup, blockquote y br, sin atributos.
                El primer autor enviado en la lista 'authorUserCodes' se convierte en el creador y Autor Principal (isMainAuthor = true) del trabajo.
                Para agregar co-autores posteriormente, se debe utilizar el endpoint de solicitudes del PaperAuthorController.
                La fecha efectiva de envío se asigna exclusivamente al enviar el trabajo a evaluación;
                los borradores no requieren estar dentro del período de presentación.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trabajo creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: congreso deshabilitado, área temática no válida, etc."),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el congreso, su configuración ABSTRACT, o el autor especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<PaperDetailResponseDTO> createPaper(@Valid @RequestBody PaperRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paperService.createPaper(dto));
    }

    @Operation(
            summary = "Agregar una palabra clave a un trabajo",
            description = "Agrega una keyword a un Paper existente. Permitido solo en estado NOT_SUBMITTED y NEEDS_REVISION."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Palabra clave agregada correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, duplicada, excede el máximo, o aparece en el título sin permitirlo"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{paperCode}/keywords")
    public ResponseEntity<Set<String>> addKeywordToPaper(
            @PathVariable String paperCode, 
            @RequestParam String keyword) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paperService.addKeywordToPaper(paperCode, keyword));
    }

    ///----------------------------------------------------------PUT----------------------------------------------------------///

    @Operation(
            summary = "Actualizar metadatos de un trabajo (Guardar Progreso)",
            description = """
                Actualiza los datos base del trabajo (Título, Resumen y Área Temática) sin enviarlo a revisión.
                No afecta a la lista de autores ni a las palabras clave.
                El resumen admite HTML restringido y sanitizado: p, strong, b, em, i, u,
                sub, sup, blockquote y br, sin atributos.
                Permitido únicamente en estado NOT_SUBMITTED o NEEDS_REVISION.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajo actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado del trabajo no editable o datos inválidos (ej. área temática incorrecta)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper o el Tipo de Trabajo")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{paperCode}")
    public ResponseEntity<PaperDetailResponseDTO> updatePaper(
            @PathVariable String paperCode, 
            @Valid @RequestBody PaperUpdateRequestDTO dto) {
        return ResponseEntity.ok(paperService.updatePaper(paperCode, dto));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    
    @Operation(
            summary = "Eliminar una palabra clave de un trabajo",
            description = "Elimina una keyword de un Paper. Permitido en NOT_SUBMITTED y NEEDS_REVISION. No se puede eliminar si es la única restante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Palabra clave eliminada correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, o es la última palabra clave"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper o la palabra clave")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{paperCode}/keywords")
    public ResponseEntity<Set<String>> removeKeywordFromPaper(
            @PathVariable String paperCode, 
            @RequestParam String keyword) {
        return ResponseEntity.ok(paperService.removeKeywordFromPaper(paperCode, keyword));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///
    
    @Operation(
            summary = "Asignar un evaluador a un trabajo",
            description = "Asigna un evaluador a un Paper. El evaluador debe pertenecer al mismo congreso que el Paper y poseer el rol EVALUADOR."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluador asignado correctamente"),
            @ApiResponse(responseCode = "400", description = "El evaluador no pertenece al mismo congreso o no posee el rol adecuado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos para realizar esta operación o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper o el evaluador"),
            @ApiResponse(responseCode = "409", description = "El Paper ya tiene este u otro evaluador asignado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{paperCode}/reviewers/{reviewerCode}")
    public ResponseEntity<MessageResponseDTO> assignReviewerToPaper(
            @PathVariable String paperCode, 
            @PathVariable String reviewerCode) {
        return ResponseEntity.ok(paperService.assignReviewerToPaper(paperCode, reviewerCode));
    }
    
    @Operation(
            summary = "Reasignar un evaluador a un trabajo",
            description = """
                    Reemplaza el evaluador actualmente asignado a un trabajo.

                    **Reglas de negocio:**
                    - El trabajo debe tener un evaluador asignado.
                    - El evaluador anterior debe haber dejado de cumplir
                      las condiciones para evaluar en el congreso.
                    - El nuevo evaluador debe tener cuenta global habilitada,
                      participación activa y rol EVALUATOR.
                    - No se permite reasignar trabajos ACCEPTED o REJECTED.
                    - No se permite reasignar trabajos con evaluaciones
                      pendientes de liberación para su versión actual.
                    - Las evaluaciones anteriores permanecen intactas.

                    **Permisos:**
                    - Administradores del congreso.
                    - Súper administradores.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluador reasignado correctamente"),
            @ApiResponse(responseCode = "400", description = "El trabajo no cumple las condiciones para ser reasignado o el nuevo evaluador no es elegible"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administración o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo o el congreso")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{paperCode}/reviewers/{newReviewerCode}/reassignment")
    public ResponseEntity<MessageResponseDTO> reassignReviewerToPaper(
            @PathVariable String paperCode,
            @PathVariable String newReviewerCode) {

        return ResponseEntity.ok(
                paperService.reassignReviewerToPaper(paperCode, newReviewerCode)
        );
    }

    @Operation(
            summary = "Enviar un trabajo a evaluación (Submit)",
            description = """
                    Transiciona el estado del Paper de NOT_SUBMITTED o NEEDS_REVISION
                    a UNDER_EVALUATION.

                    **Permisos:**
                    - Exclusivamente el autor responsable del trabajo.

                    **Reglas de negocio:**
                    - Verifica que exista al menos un autor ACCEPTED.
                    - Valida la cantidad mínima de palabras clave.
                    - Si existen solicitudes PENDING, requiere confirmación explícita
                      para descartarlas antes de enviar el trabajo.
                    - Si se confirma el descarte, las solicitudes pendientes se eliminan
                      y el trabajo se envía dentro de la misma transacción.
                    - Un envío inicial debe realizarse dentro del período de presentación del congreso.
                    - Un reenvío de corrección debe realizarse antes de su fecha límite y antes
                      del inicio del evento.
                    - La fecha efectiva de envío es asignada por el backend.

                    **Parámetro confirmDiscardPendingRequests:**
                    - false (predeterminado): no autoriza descartar solicitudes pendientes.
                    - true: confirma el descarte de todas las solicitudes pendientes.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajo enviado a revisión correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, sin autores aceptados o no alcanza el mínimo de palabras clave"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No es el autor responsable o tiene pendiente completar su institución"),
            @ApiResponse(responseCode = "404", description = "No se encontró el trabajo"),
            @ApiResponse(responseCode = "409", description = "Existen solicitudes pendientes y se requiere confirmar su descarte")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{paperCode}/submit")
    public ResponseEntity<PaperDetailResponseDTO> submitPaper(
            @PathVariable String paperCode,
            @RequestParam(defaultValue = "false")
            boolean confirmDiscardPendingRequests
    ) {
        return ResponseEntity.ok(
                paperService.submitPaper(paperCode, confirmDiscardPendingRequests)
        );
    }
}
