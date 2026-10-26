package com.tfi.gestion_congresos_backend.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.mapper.RoleMapper;
import com.tfi.gestion_congresos_backend.services.RoleService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.tfi.gestion_congresos_backend.repository.RoleRepository;


@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponseDTO> getAllRoles() {
       
        List<Role> users = roleRepository.findAll();

        //stream para transformar List<User> en List<UserResponse> 
        List<RoleResponseDTO> result = users.stream()
                                    .map(roleMapper::toRoleResponseDTO)
                                    .toList();

        return result;
    }

    @Override
    public List<RoleResponseDTO> getRegisterableRoles() {
        
        List<RoleName> registerableNames = List.of(RoleName.EXPOSITOR, RoleName.LISTENER);

        return roleRepository.findByNameIn(registerableNames)
                .stream()
                .map(roleMapper::toRoleResponseDTO)
                .toList();
    }
    
}
