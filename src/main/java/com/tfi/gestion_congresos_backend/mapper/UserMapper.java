package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
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

    ///usamos mappingtarget para que actualice y mantengan los campos que no se envían por DTO
    void updateUserFromDto(UpdateUserRequestDTO dto, @MappingTarget User user);

    // Mapeo desde el DTO administrativo a la Entidad User
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "role", ignore = true) // Se asigna manualmente en el Service por el roleId
    @Mapping(target = "password", ignore = true) // Se asigna la contraseña temporal encriptada
    @Mapping(target = "enabled", constant = "true")
    //@Mapping(target = "mustChangePassword", constant = "true")
    @Mapping(target = "dni", constant = "0L")
    @Mapping(target = "institution", constant = "PENDIENTE")
    @Mapping(target = "country", constant = "PENDIENTE")
    User toEntity(AdminCreateUserRequestDTO dto);
}