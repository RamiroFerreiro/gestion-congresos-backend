package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;

@Mapper(componentModel = "spring")
public interface PaperPaymentMapper {
    
    @Mapping(target = "paperId", source = "paper.paperId")
    PaperPaymentResponseDTO toResponseDTO(PaperPayment entity);
}
