package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UserRequest;
import com.tfi.gestion_congresos_backend.dtos.UserResponse;
import com.tfi.gestion_congresos_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    ///TEST PRUEBA  
    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("mensaje", "Hola desde Spring Boot");
    }


    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

