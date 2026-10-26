package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.EvaluationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluations", description = "Operaciones relacionadas con la evaluación de trabajos (Papers)")
public class EvaluationController {

    private final EvaluationService evaluationService;

    ///----------------------------------------------------------GETS----------------------------------------------------------///
    @Operation(
            summary = "Obtener historial de evaluaciones de un trabajo",
            description = "Obtiene la lista de evaluaciones asociadas a un Paper específico, ordenadas por fecha descendente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Evaluaciones obtenidas correctamente"
    )
    @GetMapping("/paper/{paperCode}")
    public ResponseEntity<List<EvaluationResponseDTO>> getEvaluationsByPaperCode(@PathVariable String paperCode) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByPaperCode(paperCode));
    }

    ///----------------------------------------------------------POSTS----------------------------------------------------------///
    @Operation(
            summary = "Crear una evaluación",
            description = "Crea una nueva evaluación para un Paper, aplicando el snapshot de la versión actual del trabajo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Evaluación creada correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un Paper con el código especificado"
            )
    })
    @PostMapping("/paper/{paperCode}")
    public ResponseEntity<EvaluationResponseDTO> createEvaluation(
            @PathVariable String paperCode,
            @Valid @RequestBody EvaluationRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.createEvaluation(paperCode, request));
    }

    @Operation(
            summary = "Liberar evaluaciones pendientes de un congreso (modo BATCH)",
            description = "Libera todas las evaluaciones no publicadas de un congreso en modo BATCH, marcándolas como isReleased=true y actualizando lastEvaluationReleaseDate del congreso. Rechaza la operación si el congreso está en modo IMMEDIATE."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evaluaciones liberadas correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El congreso está en modo IMMEDIATE, no hay nada que liberar manualmente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el código especificado"
            )
    })
    @PostMapping("/congress/{congressCode}/release")
    public ResponseEntity<MessageResponseDTO> releasePendingEvaluations(@PathVariable String congressCode) {
        return ResponseEntity.ok(evaluationService.releasePendingEvaluations(congressCode));
    }

}