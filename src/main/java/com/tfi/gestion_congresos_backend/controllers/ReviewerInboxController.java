package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerInboxResponseDTO;
import com.tfi.gestion_congresos_backend.enums.ReviewerInboxStatus;
import com.tfi.gestion_congresos_backend.services.ReviewerInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers/congresses/{congressCode}/reviewer-inbox")
@RequiredArgsConstructor
@Tag(name = "Evaluations", description = "Bandeja de trabajos del evaluador autenticado")
public class ReviewerInboxController {
    private final ReviewerInboxService reviewerInboxService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar mi bandeja asignada", description = "Devuelve exclusivamente las asignaciones del evaluador autenticado, ordenadas por próxima acción, con filtro opcional por estado y última actividad. No expone identidades de autores.")
    public ResponseEntity<ReviewerInboxResponseDTO> getMyInbox(
            @PathVariable String congressCode,
            @RequestParam(required = false) ReviewerInboxStatus status) {
        return ResponseEntity.ok(reviewerInboxService.getMyInbox(congressCode, status));
    }
}
