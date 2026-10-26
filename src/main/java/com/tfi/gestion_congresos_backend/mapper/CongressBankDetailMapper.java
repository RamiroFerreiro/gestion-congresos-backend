package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressBankDetail;

@Mapper(componentModel = "spring")
public interface CongressBankDetailMapper {

    // 1. DTO Request -> Entity
    // Ignoramos el id propio y la entidad Congress asociada, los asignaremos en el Service
    @Mapping(target = "congressBankDetailsId", ignore = true)
    @Mapping(target = "congress", source = "congress")
    CongressBankDetail toEntity(CongressBankDetailRequestDTO request, Congress congress);

    // 2. Entity -> DTO Response
    // Mapeamos explícitamente el ID del congreso desde la relación
    @Mapping(target = "congressId", source = "congress.congressId")
    CongressBankDetailResponseDTO toCongressBankDetailResponseDTO(CongressBankDetail congressBankDetail);

    // Mapea los datos del RequestDTO directamente sobre la entidad persistida
    @Mapping(target = "congressBankDetailsId", ignore = true)
    @Mapping(target = "congress", ignore = true)
    void updateEntityFromDto(CongressBankDetailRequestDTO dto, @MappingTarget CongressBankDetail entity);
}
