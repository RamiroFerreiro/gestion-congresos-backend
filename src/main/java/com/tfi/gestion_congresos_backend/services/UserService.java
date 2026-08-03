package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;

public interface UserService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long userId);

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    void deleteUser(Long userId);

    UserResponseDTO updateUser(Long userId, UpdateUserRequestDTO userRequestDTO);

    UserResponseDTO updateUserRole(Long userId, RoleName newRoleName);
    
    User getUserByUserId(Long userId);
    
    List<UserResponseDTO> getParticipantsByCongressAndRole(Long congressId, RoleName role);
    
    boolean existsById(Long userId);

    UserResponseDTO getAuthenticatedUser();

    //void changePassword(ChangePasswordRequestDTO request);
}