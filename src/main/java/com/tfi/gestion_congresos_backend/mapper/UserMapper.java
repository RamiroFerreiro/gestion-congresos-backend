package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    //cargamos el nombre del role en el DTO utilizando como fuente la entidad asociada
    @Mapping(target = "role", source = "role.name")
    UserResponseDTO toUserResponseDTO(User user);
    
    User toEntity(UserRequestDTO dto);

    ///usamos mappingtarget para que actualice y mantengan los campos que no envían por DTO
    void updateUserFromDto(UpdateUserRequestDTO dto, @MappingTarget User user);

}