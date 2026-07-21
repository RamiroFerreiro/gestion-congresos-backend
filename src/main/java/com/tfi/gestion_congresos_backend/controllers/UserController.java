package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UserRequest;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    ///TEST PRUEBA  
    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("mensaje", "Hola desde Spring Boot");
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        //Response entity para controlar el codigo HTTP
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequest request) {

        UserResponseDTO response = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

