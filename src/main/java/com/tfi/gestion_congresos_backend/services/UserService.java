package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;


public interface UserService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long userId);
}