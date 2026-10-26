package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;

import java.util.List;

public interface EvaluationService {
    
    // Método para obtener el historial de evaluaciones de un paper
    List<EvaluationResponseDTO> getEvaluationsByPaperCode(String paperCode);
    
    // Metodo para crear una nueva evaluacion
    EvaluationResponseDTO createEvaluation(String paperCode, EvaluationRequestDTO requestDTO);

    // Liberar Evaluaciones por Congreso
    MessageResponseDTO releasePendingEvaluations(String congressCode);
}