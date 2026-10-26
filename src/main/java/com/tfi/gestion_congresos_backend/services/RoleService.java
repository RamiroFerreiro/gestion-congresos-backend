package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;


public interface RoleService {

    List<RoleResponseDTO> getAllRoles();
    
    List<RoleResponseDTO> getRegisterableRoles();
    
} 