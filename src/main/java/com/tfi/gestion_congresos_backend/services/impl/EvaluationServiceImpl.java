package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.mapper.EvaluationMapper;
import com.tfi.gestion_congresos_backend.repository.EvaluationRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.services.EvaluationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final PaperRepository paperRepository;
    private final EvaluationMapper evaluationMapper;

    public EvaluationServiceImpl(EvaluationRepository evaluationRepository, PaperRepository paperRepository, EvaluationMapper evaluationMapper) {

        this.evaluationRepository = evaluationRepository;
        this.paperRepository = paperRepository;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> getEvaluationsByPaperId(Long paperId) {

        List<Evaluation> evaluations = evaluationRepository.findByPaper_PaperIdOrderByEvaluationDateDesc(paperId);
        
        return evaluations.stream()
                .map(evaluationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public EvaluationResponseDTO createEvaluation(Long paperId, EvaluationRequestDTO requestDTO) {
        // Buscar el Paper
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper no encontrado con ID: " + paperId));

        // Mapear los datos que vienen del DTO (feedback, newDeadline, newStatus)
        Evaluation evaluation = evaluationMapper.toEntity(requestDTO);

        // Aplicar lógica de negocio (Snapshot)
        evaluation.setPaper(paper);
        evaluation.setEvaluatedVersion(paper.getVersion()); // Copiamos la versión actual
        evaluation.setEvaluationDate(LocalDateTime.now()); // Fecha de la evaluación
        
        // Actualizar el Paper con el nuevo estado
        paper.setStatus(requestDTO.getNewStatus());

        // 5. Guardar la evaluación. (Como seteamos el status del paper acá arriba, 
        // y como estás dentro de un @Transactional, Hibernate actualizará el Paper automáticamente 
        // al finalizar el método).
        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        // Devolver la respuesta mapeada
        return evaluationMapper.toResponseDTO(savedEvaluation);
    }
}