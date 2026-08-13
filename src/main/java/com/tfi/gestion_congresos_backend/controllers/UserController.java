package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UpdateUserRoleRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Operaciones relacionadas con la gestión de usuarios")
public class UserController {

    private final UserService userService;

    ///----------------------------------------------------------TEST PRUEBA----------------------------------------------------------///
    @Operation(
            summary = "Endpoint de prueba",
            description = "Endpoint utilizado para verificar que la API se encuentra disponible."
    )
    @ApiResponse(
            responseCode = "200",
            description = "La API respondió correctamente"
    )  
    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("mensaje", "Hola desde Spring Boot");
    }

    ///----------------------------------------------------------GETS----------------------------------------------------------///
    @Operation(
            summary = "Obtener todos los usuarios",
            description = "Obtiene la lista de todos los usuarios registrados en el sistema."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuarios obtenidos correctamente"
    )
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        //Response entity para controlar el codigo HTTP
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
            summary = "Obtener un usuario por ID",
            description = "Obtiene la información de un usuario a partir de su identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario con el ID especificado"
            )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId){

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Operation(
            summary = "Obtener usuario autenticado",
            description = "Obtiene la información del usuario actualmente autenticado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario autenticado obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "El usuario no se encuentra autenticado"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getAuthenticatedUser( ){

        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }



    ///----------------------------------------------------------POSTS----------------------------------------------------------///
    @Operation(
            summary = "Crear un usuario",
            description = "Registra un nuevo usuario en el sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El rol especificado no fue encontrado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un usuario con el email indicado"
            )
    })
    @SecurityRequirement(name = "")
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    @Operation(
            summary = "Eliminar un usuario",
            description = "Deshabilita un usuario existente mediante una eliminación lógica."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Usuario eliminado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario con el ID especificado"
            )
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }


    ///----------------------------------------------------------PUT----------------------------------------------------------///
    @Operation(
            summary = "Actualizar un usuario",
            description = "Actualiza la información de un usuario existente a partir de su identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el usuario o el rol especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un usuario con el email indicado"
            )
    })
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequestDTO request) {

        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///
    @Operation(
            summary = "Actualizar el rol de un usuario",
            description = "Modifica el rol asignado a un usuario existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Rol del usuario actualizado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró el usuario o el rol especificado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El rol enviado no es válido"
            )
    })
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequestDTO request) {

        return ResponseEntity.ok(userService.updateUserRole(userId, request.getRoleName()));
    }

    @Operation(
            summary = "Cambiar contraseña",
            description = "Permite al usuario autenticado cambiar su contraseña."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña actualizada correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "El usuario no se encuentra autenticado"
            )
    })
    @PatchMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {

        return ResponseEntity.ok(userService.changePassword(request));
    }

        
}

