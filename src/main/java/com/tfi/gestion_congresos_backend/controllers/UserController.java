package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UpdateUserRoleRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.AuthService;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
            summary = "Obtener un usuario por código",
            description = "Obtiene la información de un usuario a partir de su código."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un usuario con el código especificado"
            )
    })
    @GetMapping("/{code}")
    public ResponseEntity<UserResponseDTO> getUserByCode(@PathVariable String code){

        return ResponseEntity.ok(userService.getUserByCode(code));
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
            summary = "Alta de usuario por Administrador",
            description = "Permite a un administrador crear un usuario con datos mínimos. Genera una contraseña provisoria y la envía por e-mail."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado y correo enviado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o e-mail ya existente"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador")
    })
    @PostMapping("/admin/create")
    public ResponseEntity<UserResponseDTO> adminCreateUser(@Valid @RequestBody AdminCreateUserRequestDTO request) {
        UserResponseDTO response = userService.adminCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



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
                    description = "No se encontró un usuario con el código especificado"
            )
    })
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteUser(@PathVariable String code) {

        userService.deleteUser(code);

        return ResponseEntity.noContent().build();
    }


    ///----------------------------------------------------------PUT----------------------------------------------------------///
    @Operation(
            summary = "Actualizar un usuario",
            description = "Actualiza la información de un usuario existente a partir de su código."
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
    @PutMapping("/{code}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable String code, @Valid @RequestBody UpdateUserRequestDTO request) {

        return ResponseEntity.ok(userService.updateUser(code, request));
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
    @PatchMapping("/{code}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable String code, @Valid @RequestBody UpdateUserRoleRequestDTO request) {

        return ResponseEntity.ok(userService.updateUserRole(code, request.getRoleId()));
    }

    @Operation(
            summary = "Cambiar o actualizar contraseña",
            description = "Permite al usuario autenticado cambiar su contraseña actual por una nueva. " +
                          "Este mismo endpoint atiende tanto el cambio voluntario desde el perfil como el primer ingreso obligatorio " +
                          "tras la asignación de una contraseña provisoria por un administrador (desactivando el indicador mustChangePassword)."
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

    @Operation(
            summary = "Cambiar email",
            description = "Permite al usuario autenticado cambiar su email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email de recuperación enviado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "El usuario no se encuentra autenticado o la contraseña es incorrecta"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe un usuario con el email indicado"
            )
    })
    @PatchMapping("/change-email")
    public ResponseEntity<MessageResponseDTO> changeEmail(@Valid @RequestBody ChangeEmailRequestDTO request) {

        return ResponseEntity.ok(userService.changeEmail(request));
    }

}

