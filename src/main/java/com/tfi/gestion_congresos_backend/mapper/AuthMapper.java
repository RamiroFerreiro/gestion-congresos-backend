package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tfi.gestion_congresos_backend.dtos.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    //cargamos el nombre del role en el DTO utilizando como fuente la entidad asociada
    @Mapping(target = "role", source = "role.name")
    LoginResponseDTO toLoginResponseDTO(User user);
} 
