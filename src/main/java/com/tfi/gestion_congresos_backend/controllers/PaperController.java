package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @Operation(
            summary = "Obtener trabajos asignados a un evaluador",
            description = "Obtiene la lista de Papers que tiene asignados un evaluador específico, para que pueda visualizar únicamente los trabajos que le corresponde revisar."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trabajos obtenidos correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un evaluador con el ID especificado"
            )
    })
    @GetMapping("/reviewer/{reviewerId}")
    public ResponseEntity<List<PaperResponseDTO>> getAssignedPapers(@PathVariable Long reviewerId) {
        List<PaperResponseDTO> papers = paperService.getAssignedPapers(reviewerId);
        return ResponseEntity.ok(papers);
    }

    /// Obtener trabajos de un congreso:
    @Operation(
            summary = "Obtener todos los trabajos de un congreso",
            description = "Obtiene la lista de todos los trabajos de un congreso."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Trabajos obtenidos correctamente"
    )
    @GetMapping
	public ResponseEntity<List<PaperResponseDTO>> getPapersByCongress(@RequestParam Long congressId) {
		
		return ResponseEntity.ok(paperService.getPapersByCongressId(congressId));
	}
    
    /// Asignar un evaluador a un trabajo:
    @Operation(
            summary = "Asignar un evaluador a un trabajo",
            description = "Otorga un evaluador a un trabajo existente."
    )
    @ApiResponses({
    		@ApiResponse(
    				responseCode = "200",
    				description = "Evaluador asignado correctamente"
    		),
    		@ApiResponse(
    				responseCode = "400",
    				description = "El trabajo y el evaluador no pertenecen al mismo congreso"
    		),
    		@ApiResponse(
                    responseCode = "409",
                    description = "Conflicto al asignar el evaluador. Razones posibles:\n" +
                                  "- El trabajo ya tiene asignado un evaluador diferente.\n" +
                                  "- El evaluador seleccionado ya estaba asignado a este trabajo."
            )
    })
    @PatchMapping("/{paperId}/reviewers/{reviewerId}")
    public ResponseEntity<MessageResponseDTO> assingReviewerToPaper(@PathVariable Long paperId, @PathVariable Long reviewerId) {
    	
    	return ResponseEntity.ok(paperService.assignReviewerToPaper(paperId, reviewerId));
    }

    @Operation(
        summary = "Crear un trabajo",
        description = "Crea un nuevo Paper, el cual nace en estado NOT_SUBMITTED. El código de trackeo se genera automáticamente. Requiere al menos un autor inscripto con rol EXPOSITOR en el congreso (o ADMINISTRATOR), un área temática válida para el congreso, y una fecha de presentación dentro de la ventana habilitada. El congreso debe estar habilitado (enabled = true)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Trabajo creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos: congreso deshabilitado, área temática no válida, fecha fuera de la ventana de presentación, exceso de autores, o algún autor sin rol EXPOSITOR en el congreso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el congreso o alguno de los autores especificados"
            )
    })
    @PostMapping
    public ResponseEntity<PaperResponseDTO> createPaper(@Valid @RequestBody PaperRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paperService.createPaper(dto));
    }

    @Operation(
            summary = "Agregar un autor a un trabajo",
            description = "Agrega un usuario como autor de un Paper existente. Permitido únicamente en estado NOT_SUBMITTED. El usuario debe tener rol EXPOSITOR inscripto en el congreso del Paper (o ser ADMINISTRATOR), y no puede superar el máximo de autores configurado en el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Autor agregado correctamente. Devuelve la lista actualizada de autores"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El Paper no está en estado NOT_SUBMITTED, se superó el máximo de autores del congreso, o el usuario no tiene rol EXPOSITOR"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el Paper o el usuario especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El usuario ya es autor de este Paper"
            )
    })
    @PostMapping("/{paperId}/authors/{userId}")
    public ResponseEntity<List<AuthorResponseDTO>> addAuthorToPaper(
            @PathVariable Long paperId,
            @PathVariable Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperService.addAuthorToPaper(paperId, userId));
    }

    @Operation(
            summary = "Enviar un trabajo a revisión",
            description = "Transiciona el estado del Paper de NOT_SUBMITTED o NEEDS_REVISION a UNDER_EVALUATION. Requiere al menos un autor cargado y que la cantidad de palabras clave cumpla el mínimo configurado en el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trabajo enviado a revisión correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El Paper no está en un estado válido para ser enviado, no tiene autores cargados, o no alcanza el mínimo de palabras clave del congreso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un Paper con el ID especificado"
            )
    })
    @PatchMapping("/{paperId}/submit")
    public ResponseEntity<PaperResponseDTO> submitPaper(@PathVariable Long paperId) {
        return ResponseEntity.ok(paperService.submitPaper(paperId));
    }


    @Operation(
        summary = "Eliminar un autor de un trabajo",
        description = "Elimina la relación entre un usuario y un Paper. Permitido únicamente en estado NOT_SUBMITTED. El autor con orden 1 (creador del Paper) nunca puede eliminarse, garantizando que el Paper siempre tenga al menos un autor. Reordena el authorOrder de los autores restantes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autor eliminado correctamente. Devuelve la lista actualizada de autores"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El Paper no está en estado NOT_SUBMITTED, o se intentó eliminar al autor creador (orden 1)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el Paper, o el usuario no es autor de este Paper"
            )
    })
    @DeleteMapping("/{paperId}/authors/{userId}")
    public ResponseEntity<List<AuthorResponseDTO>> removeAuthorFromPaper(
            @PathVariable Long paperId,
            @PathVariable Long userId) {

        return ResponseEntity.ok(paperService.removeAuthorFromPaper(paperId, userId));
    }

    @Operation(
        summary = "Agregar una palabra clave a un trabajo",
        description = "Agrega una keyword a un Paper existente. Permitido en NOT_SUBMITTED y NEEDS_REVISION. Rechaza duplicados (case-insensitive) y valida contra maxKeywords y keywordRepetition del congreso."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Palabra clave agregada correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, duplicada, excede el máximo, o aparece en el título sin permitirlo"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper")
    })
    @PostMapping("/{paperId}/keywords")
    public ResponseEntity<Set<String>> addKeywordToPaper(
            @PathVariable Long paperId,
            @RequestParam String keyword) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperService.addKeywordToPaper(paperId, keyword));
    }

    @Operation(
            summary = "Eliminar una palabra clave de un trabajo",
            description = "Elimina una keyword de un Paper. Permitido en NOT_SUBMITTED y NEEDS_REVISION. No se puede eliminar si es la última palabra clave restante."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Palabra clave eliminada correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido, o es la última palabra clave del Paper"),
            @ApiResponse(responseCode = "404", description = "No se encontró el Paper o la palabra clave especificada")
    })
    @DeleteMapping("/{paperId}/keywords")
    public ResponseEntity<Set<String>> removeKeywordFromPaper(
            @PathVariable Long paperId,
            @RequestParam String keyword) {

        return ResponseEntity.ok(paperService.removeKeywordFromPaper(paperId, keyword));
    }
}