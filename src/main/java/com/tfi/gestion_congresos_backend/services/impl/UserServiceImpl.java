package com.tfi.gestion_congresos_backend.services.impl;


import com.tfi.gestion_congresos_backend.dtos.UserRequest;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponseDTO createUser(UserRequest request) {

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();

        User savedUser = userRepository.save(user);

        return UserResponseDTO.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    public List<UserResponseDTO> getAllUsers(){
        
        List<User> users = userRepository.findAll();

        //stream para transformar List<User> en List<UserResponse> 
        List<UserResponseDTO> result = users.stream()
                                    .map(userMapper::toUserResponseDTO)
                                    .toList();

        return result;
    }
}