package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<LoginResponseDTO> createUser(@RequestBody LoginRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(authService.login(request));
    }
}
