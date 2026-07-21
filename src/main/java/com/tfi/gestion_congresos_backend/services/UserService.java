package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.UserRequest;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;


public interface UserService {

    UserResponseDTO createUser(UserRequest request);
    List<UserResponseDTO> getAllUsers();

}