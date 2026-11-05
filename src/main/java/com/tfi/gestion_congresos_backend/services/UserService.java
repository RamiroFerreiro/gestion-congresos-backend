package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.CompleteFirstLoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;

public interface UserService {

    ///-----------------------------------------------------CREATE-----------------------------------------------------///
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO adminCreateUser(String congressCode, AdminCreateUserRequestDTO request);
    
    MessageResponseDTO completeFirstLogin(CompleteFirstLoginRequestDTO request);

    ///-----------------------------------------------------GET-----------------------------------------------------///

    ///Controllers
    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getAuthenticatedUser();

    //Auxiliares
    UserResponseDTO getUserById(Long userId); 

    User getUserByUserId(Long userId); 

    User getUserByUserCode(String code);

    ///-----------------------------------------------------DELETE-----------------------------------------------------///

    void deleteUser(String userCode);

    ///-----------------------------------------------------UPDATE-----------------------------------------------------///

    UserResponseDTO updateUser(String userCode, UpdateUserRequestDTO userRequestDTO);

    MessageResponseDTO changePassword(ChangePasswordRequestDTO request); 

    MessageResponseDTO changeEmail(ChangeEmailRequestDTO request); 

    ///BOOLEAN
    boolean existsByCode(String code);


    
}