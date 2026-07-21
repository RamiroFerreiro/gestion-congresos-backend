package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toUserResponseDTO(User user);

}