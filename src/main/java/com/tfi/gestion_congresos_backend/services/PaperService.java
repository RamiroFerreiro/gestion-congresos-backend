package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;

import java.util.List;

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
    void assignReviewerToPaper(Long paperId, Long reviewerId);
    
}