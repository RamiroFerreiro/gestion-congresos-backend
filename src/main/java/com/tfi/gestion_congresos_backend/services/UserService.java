package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;

public interface UserService {

    ///GET

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long userId);

    User getUserByUserId(Long userId);

    UserResponseDTO getAuthenticatedUser();

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    List<UserResponseDTO> getParticipantsByCongressAndRole(Long congressId, RoleName role);

    ///DELETE

    void deleteUser(Long userId);

    ///UPDATE

    UserResponseDTO updateUser(Long userId, UpdateUserRequestDTO userRequestDTO);

    UserResponseDTO updateUserRole(Long userId, RoleName newRoleName);

    MessageResponseDTO changePassword(ChangePasswordRequestDTO request);
    
    ///BOOLEAN
    
    boolean existsById(Long userId);

    
    
}