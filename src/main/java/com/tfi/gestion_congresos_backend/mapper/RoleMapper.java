package com.tfi.gestion_congresos_backend.mapper;

import org.mapstruct.Mapper;

import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    
    RoleResponseDTO toRoleResponseDTO(Role role);
}
