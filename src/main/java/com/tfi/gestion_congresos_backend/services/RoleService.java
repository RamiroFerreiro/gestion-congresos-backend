package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;


public interface RoleService {

    List<RoleResponseDTO> getAllRoles();
    
    List<RoleResponseDTO> getRegisterableRoles();
    
    void validateAssignableRole(RoleName role);
} 