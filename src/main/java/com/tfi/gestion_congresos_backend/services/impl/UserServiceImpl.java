package com.tfi.gestion_congresos_backend.services.impl;


import com.tfi.gestion_congresos_backend.dtos.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.EmailAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponseDTO> getAllUsers(){
        
        List<User> users = userRepository.findAll();

        //stream para transformar List<User> en List<UserResponse> 
        List<UserResponseDTO> result = users.stream()
                                    .map(userMapper::toUserResponseDTO)
                                    .toList();

        return result;
    }

    @Override
    public UserResponseDTO getUserById(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));
        
        UserResponseDTO result = userMapper.toUserResponseDTO(user);

        return result;
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO request){

        //verificamos que no esté registrado el mail
        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException( "Ya existe un usuario con ese email.");
        }

        //mapeamos DTO a entidad
        User user = userMapper.toEntity(request);

        //buscamos el rol, si existe seteamos y luego guardamos en bd
        Role role = roleRepository.findById(request.getRoleId()).orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        user.setRole(role);
        user.setEnabled(true);

        user = userRepository.save(user);

        //retorno de entidad a DTO
        UserResponseDTO result = userMapper.toUserResponseDTO(user);
        return result;
    }

    @Override
    public void deleteUser(Long userId) {
    
        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));

        user.setEnabled(false);


        userRepository.save(user);
    }

}