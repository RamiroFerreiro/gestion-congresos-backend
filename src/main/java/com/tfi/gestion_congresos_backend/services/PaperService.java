package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import java.util.List;

public interface PaperService {

    /// Busca todos los trabajos asignados al ID de un evaluador
    List<PaperResponseDTO> getAssignedPapers(Long reviewerId);
    
}