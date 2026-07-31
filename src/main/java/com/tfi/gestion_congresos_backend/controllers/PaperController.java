package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

}