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


@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    // El evaluador visualiza únicamente los trabajos que tenga asignados
    // Mapea la petición HTTP GET a /api/papers/reviewer/{reviewerId}
    @GetMapping("/reviewer/{reviewerId}") 
    // Extrae el parámetro reviewerId de la URL
    public ResponseEntity<List<PaperResponseDTO>> getAssignedPapers(@PathVariable Long reviewerId) { 
        // Llama al servicio para obtener la lista de DTOs
        List<PaperResponseDTO> papers = paperService.getAssignedPapers(reviewerId); 
        // Retorna la lista dentro de una respuesta HTTP 200 OK
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

    /// Crear Paper -> nace en estado NOT_SUBMITTED
    @PostMapping
    public ResponseEntity<PaperResponseDTO> createPaper(@Valid @RequestBody PaperRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paperService.createPaper(dto));
    }

    ///Enviar Paper -> NOT_SUBMITTED/NEEDS_REVISION -> UNDER_EVALUATION
    @PatchMapping("/{paperId}/submit")
    public ResponseEntity<PaperResponseDTO> submitPaper(@PathVariable Long paperId) {
        return ResponseEntity.ok(paperService.submitPaper(paperId));
    }

    /// Agregar un autor a un Paper (solo en estado NOT_SUBMITTED):
    @PostMapping("/{paperId}/authors/{userId}")
    public ResponseEntity<List<AuthorResponseDTO>> addAuthorToPaper(
            @PathVariable Long paperId,
            @PathVariable Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paperService.addAuthorToPaper(paperId, userId));
    }

    /// Eliminar un autor de un Paper (solo en NOT_SUBMITTED, nunca al autor con orden 1):
    @DeleteMapping("/{paperId}/authors/{userId}")
    public ResponseEntity<List<AuthorResponseDTO>> removeAuthorFromPaper(
            @PathVariable Long paperId,
            @PathVariable Long userId) {

        return ResponseEntity.ok(paperService.removeAuthorFromPaper(paperId, userId));
    }
}