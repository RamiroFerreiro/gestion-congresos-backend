package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperUpdateRequestDTO;
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
            description = "Obtiene la lista resumida de Papers que tiene asignados un evaluador específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "No se encontró un evaluador con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/reviewer/{reviewerCode}")
    public ResponseEntity<List<PaperSummaryResponseDTO>> getAssignedPapers(@PathVariable String reviewerCode) {
        return ResponseEntity.ok(paperService.getAssignedPapers(reviewerCode));
    }

    @Operation(
            summary = "Obtener todos los trabajos de un congreso",
            description = "Obtiene la lista resumida de todos los trabajos de un congreso, identificado por su código."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
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
                El primer autor enviado en la lista 'authorUserCodes' se convierte en el creador y Autor Principal (isMainAuthor = true) del trabajo.
                Para agregar co-autores posteriormente, se debe utilizar el endpoint de solicitudes del PaperAuthorController.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trabajo creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: congreso deshabilitado, área temática no válida, fecha fuera de ventana, etc."),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "No se encontró el congreso, el tipo de trabajo, o el autor especificado")
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
                Actualiza los datos base del trabajo (Título, Resumen, Área Temática, Fecha y Tipo) sin enviarlo a revisión.
                No afecta a la lista de autores ni a las palabras clave.
                Permitido únicamente en estado NOT_SUBMITTED o NEEDS_REVISION.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajo actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado del trabajo no editable o datos inválidos (ej. área temática incorrecta)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
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
            summary = "Enviar un trabajo a revisión (Submit)",
            description = """
                Transiciona el estado del Paper de NOT_SUBMITTED o NEEDS_REVISION a UNDER_EVALUATION.
                Verifica que cumpla los requisitos mínimos (ej. cantidad mínima de palabras clave y autores).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajo enviado a revisión correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, sin autores, o no alcanza el mínimo de palabras clave"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "No se encontró un Paper con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{paperCode}/submit")
    public ResponseEntity<PaperDetailResponseDTO> submitPaper(@PathVariable String paperCode) {
        return ResponseEntity.ok(paperService.submitPaper(paperCode));
    }
}