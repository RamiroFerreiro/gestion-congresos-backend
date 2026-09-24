package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;

public interface UserService {

    /// CREATE
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO adminCreateUser(AdminCreateUserRequestDTO request);

    ///GET

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long userId);

    User getUserByUserId(Long userId);
    
    UserResponseDTO getUserByCode(String code);

    User getUserByUserCode(String code);

    UserResponseDTO getAuthenticatedUser();

    User getAuthenticatedUserEntity();

    List<UserResponseDTO> getParticipantsByCongressAndRole(String code, RoleName role);

    ///DELETE

    void deleteUser(String code);

    ///UPDATE

    UserResponseDTO updateUser(String code, UpdateUserRequestDTO userRequestDTO);

    UserResponseDTO updateUserRole(String code, Long roleID);

    MessageResponseDTO changePassword(ChangePasswordRequestDTO request);

    MessageResponseDTO changeEmail(ChangeEmailRequestDTO request);

    ///BOOLEAN
    
    boolean existsByCode(String code);
}