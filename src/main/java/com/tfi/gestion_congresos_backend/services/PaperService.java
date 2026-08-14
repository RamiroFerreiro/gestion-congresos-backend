package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;

import java.util.List;

// Se deben implementar metodos CRUD
// (C): Crean un nuevo registro en la BD
// (R): Lee un registro existente en la BD
// (U): Actualiza un registro existente en la BD
// (D): Elimina un registro existente en la BD

public interface PaperService {

    /// Busca todos los trabajos asignados al ID de un evaluador
    List<PaperResponseDTO> getAssignedPapers(Long reviewerId);
    
    /// Obtener un paper por su ID:
    PaperResponseDTO getPaperById(Long paperId);
    
    /// Obtener una entidad paper por su ID:
    Paper getPaperByPaperId(Long paperId);
    
    /// Obtener todos los trabajos de un congreso por su ID:
    List<PaperResponseDTO> getPapersByCongressId(Long congressId);
    
    /// Asignar un evaluador a un trabajo:
    MessageResponseDTO assignReviewerToPaper(Long paperId, Long reviewerId);

    // Crear paper (C)
    PaperResponseDTO createPaper(PaperRequestDTO dto);
    
    // Enviar paper (U)
    PaperResponseDTO submitPaper(Long paperId);
    
    // Crear Autor en paper (C)
    List<AuthorResponseDTO> addAuthorToPaper(Long paperId, Long userId);

    // Remover Autor en paper (D)
    List<AuthorResponseDTO> removeAuthorFromPaper(Long paperId, Long userId);

}