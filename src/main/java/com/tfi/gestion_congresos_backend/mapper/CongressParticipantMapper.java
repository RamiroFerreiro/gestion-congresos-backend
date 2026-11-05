package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantResponseDTO;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CongressParticipantMapper {

    @Mapping(target = "user", source = "user")
    @Mapping(target = "role", source = "role.name")
    CongressParticipantResponseDTO toCongressParticipantResponseDTO(
            CongressParticipant participant
    );
}