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

    ///-----------------------------------------------------CREATE-----------------------------------------------------///
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO adminCreateUser(AdminCreateUserRequestDTO request); //check

    ///-----------------------------------------------------GET-----------------------------------------------------///

    ///Controllers
    List<UserResponseDTO> getAllUsers();

    List<UserResponseDTO> getUsersByCongress(String congressCode);

    UserResponseDTO getUserByCodeAndCongress(String congressCode, String userCode);

    UserResponseDTO getAuthenticatedUser();

    //Auxiliares
    UserResponseDTO getUserById(Long userId); 

    User getUserByUserId(Long userId); 

    User getUserByUserCode(String code); 

    User getAuthenticatedUserEntity(); 

    List<UserResponseDTO> getParticipantsByCongressAndRole(String code, RoleName role); //probar

    ///-----------------------------------------------------DELETE-----------------------------------------------------///

    void deleteUser(String userCode);

    ///-----------------------------------------------------UPDATE-----------------------------------------------------///

    UserResponseDTO updateUser(String userCode, UpdateUserRequestDTO userRequestDTO);

    void updateUserRole(String congressCode, String userCode, RoleName newRole); 

    MessageResponseDTO changePassword(ChangePasswordRequestDTO request); 

    MessageResponseDTO changeEmail(ChangeEmailRequestDTO request); 

    ///BOOLEAN
    boolean existsByCode(String code);


    
}