package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CongressMapper {
	
	// De entidad a DTO de respuesta:
    CongressResponseDTO toCongressResponseDTO(Congress congress);

    // De DTO de creación a entidad:
    Congress toEntity(CongressRequestDTO dto);

    // Actualización de una entidad existente desde un DTO:
    void updateCongressFromDto(CongressRequestDTO dto, @MappingTarget Congress congress);
}
