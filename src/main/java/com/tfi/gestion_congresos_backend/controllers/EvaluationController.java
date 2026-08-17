package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.services.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    ///GET
    ///Traer el historial de evaluaciones de un Paper específico
    @GetMapping("/paper/{paperId}")
    public ResponseEntity<List<EvaluationResponseDTO>> getEvaluationsByPaperId(@PathVariable Long paperId) {
        return ResponseEntity.ok(evaluationService.getEvaluationsByPaperId(paperId));
    }

    ///POST
    ///Crear una nueva evaluación para un Paper
    @PostMapping("/paper/{paperId}")
    public ResponseEntity<EvaluationResponseDTO> createEvaluation(
            @PathVariable Long paperId, 
            @Valid @RequestBody EvaluationRequestDTO request) {
            
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.createEvaluation(paperId, request));
    }

}