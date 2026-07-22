package com.tfi.gestion_congresos_backend.services.impl;


import com.tfi.gestion_congresos_backend.dtos.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
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

    public UserResponseDTO getUserById(Long userId){

        User user = userRepository.findById(userId).orElseThrow(() ->
                    new ResourceNotFoundException( "Usuario no encontrado con ID: " + userId));
        
        UserResponseDTO result = userMapper.toUserResponseDTO(user);

        return result;
    }

}