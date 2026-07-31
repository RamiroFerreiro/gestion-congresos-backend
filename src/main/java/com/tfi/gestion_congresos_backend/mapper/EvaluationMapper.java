package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.tfi.gestion_congresos_backend.dtos.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Evaluation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaluationMapper {

    // De Entidad a DTO de Respuesta
    EvaluationResponseDTO toResponseDTO(Evaluation evaluation);

    // De Request DTO a Entidad
    Evaluation toEntity(EvaluationRequestDTO dto);
}