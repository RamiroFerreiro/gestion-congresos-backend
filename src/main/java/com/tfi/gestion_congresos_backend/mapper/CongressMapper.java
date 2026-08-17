package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.tfi.gestion_congresos_backend.dtos.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;

@Mapper(componentModel = "spring", uses = {UserMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CongressMapper {
	
	// De entidad a DTO de respuesta:
    // MapStruct mapea automáticamente "Set<User> participants" a "Set<UserResponseDTO> participants" usando el UserMapper que le indicamos en 'uses'.
    CongressResponseDTO toCongressResponseDTO(Congress congress);

    // De DTO de creación a entidad:
    Congress toEntity(CongressRequestDTO dto);

    // Actualización de una entidad existente desde un DTO:
    void updateCongressFromDto(CongressRequestDTO dto, @MappingTarget Congress congress);
}
