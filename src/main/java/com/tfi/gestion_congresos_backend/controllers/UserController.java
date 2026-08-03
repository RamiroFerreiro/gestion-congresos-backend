package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UpdateUserRoleRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    ///GET
    ///Traer a todos los usuarios 
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        //Response entity para controlar el codigo HTTP
        return ResponseEntity.ok(userService.getAllUsers());
    }

    ///Traer un usuario por ID
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId){

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    ///Traer un usuario por ID
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getAuthenticatedUser( ){

        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }



    ///POST
    ///Crear un usuario
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    ///DELETE
    ///Borrar un usuario
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }


    ///PUT
    ///Actualizar un usuario
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequestDTO request) {

        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    ///PATCH
    ///Actualizar el rol de un usuario
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequestDTO request) {

        return ResponseEntity.ok(userService.updateUserRole(userId, request.getRoleName()));
    }

    ///Cambiar contraseña
    @PatchMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {

        return ResponseEntity.ok(userService.changePassword(request));
    }
        
}

