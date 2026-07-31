package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.EvaluationResponseDTO;
import java.util.List;

public interface EvaluationService {
    
    // Método para obtener el historial de evaluaciones de un paper
    List<EvaluationResponseDTO> getEvaluationsByPaperId(Long paperId);
    
    // Metodo para crear una nueva evaluacion
    public EvaluationResponseDTO createEvaluation(Long paperId, EvaluationRequestDTO requestDTO);


}