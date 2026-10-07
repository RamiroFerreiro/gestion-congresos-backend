package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.UpdateUserRoleRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ChangeEmailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.AdminCreateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.ChangePasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UpdateUserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;
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
    @ApiResponse(responseCode = "200",description = "La API respondió correctamente")  
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("mensaje", "Hola desde Spring Boot");
    }

    ///----------------------------------------------------------GETS----------------------------------------------------------///
    @Operation(
        summary = "Obtener el listado global de usuarios del sistema (Solo Súper Admin)",
        description = """
            Recupera la lista completa de todos los usuarios registrados en la plataforma.
            
            **Políticas de Control de Acceso:**
            - **Súper Admin Exclusivo:** Requiere obligatoriamente `isSuperAdmin = true`.
            - **Cualquier otro rol:** Denegado con `403 Forbidden`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado global recuperado exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos de administración global (Requiere Súper Admin)")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
        summary = "Obtener los usuarios de un congreso específico",
        description = """
            Devuelve el listado de usuarios participantes pertenecientes de forma exclusiva al congreso indicado.
            
            **Políticas de Control de Acceso:**
            - **Súper Admin:** Puede consultar el listado de usuarios de cualquier congreso.
            - **Admin del Congreso:** Requiere poseer obligatoriamente el rol `ADMINISTRATOR` en estado activo sobre el `congressCode` enviado.
            - **Otros roles / Sin relación:** Denegado con `403 Forbidden`.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de participantes del congreso recuperado exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-congress")
    public ResponseEntity<List<UserResponseDTO>> getUsersByCongress(@RequestParam String congressCode) {
        return ResponseEntity.ok(userService.getUsersByCongress(congressCode));
    }

    @Operation(
        summary = "Obtener el detalle de un usuario en un congreso específico",
        description = """
            Recupera la información de un usuario asegurando que pertenezca al congreso indicado.
            
            **Secuencia de Validaciones:**
            1. Verifica la existencia del usuario objetivo (`404 Not Found`).
            2. Otorga acceso directo si el solicitante es Súper Admin.
            3. Valida que el solicitante sea Administrador activo del `congressCode` enviado (`403 Forbidden`).
            4. Valida que el usuario objetivo participe en ese mismo congreso (`404 Not Found`).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tienes permisos de administración sobre el congreso especificado"),
        @ApiResponse(responseCode = "404", description = "No se encontró un usuario activo con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{userCode}")
    public ResponseEntity<UserResponseDTO> getUserByCodeAndCongress(@PathVariable String userCode, @RequestParam String congressCode){

        return ResponseEntity.ok(userService.getUserByCodeAndCongress(congressCode, userCode));
    }

    @Operation(
        summary = "Obtener usuario autenticado",
        description = "Obtiene la información del usuario actualmente autenticado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario autenticado obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "El usuario no se encuentra autenticado")
    })
    @PreAuthorize("isAuthenticated()")
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
        summary = "Registrar un nuevo usuario en la plataforma",
        description = """
            Crea la **identidad global** de un nuevo usuario en la plataforma de gestión de congresos.
            
            **Flujo y Lógica de Negocio:**
            1. **Validación de credenciales:** Comprueba que la contraseña y su confirmación coincidan exactamente.
            2. **Unicidad de cuenta:** Verifica que no exista un usuario registrado previamente con el mismo correo electrónico.
            3. **Asignación de atributos de identidad:** 
               - Genera automáticamente un código único de usuario (userCode).
               - Habilita la cuenta globalmente (enabled = true).
               - Encripta la contraseña de forma segura mediante BCrypt.
            4. **Desacoplamiento Contextual:** 
               - Este registro crea únicamente la **cuenta global** del usuario.
               - No asigna roles globales ni afiliación institucional, ya que estos se gestionan de forma contextual en cada congreso a través de la relación de participación.
            5. Endpoint público (no requiere token previo).
               """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
        @ApiResponse(responseCode = "409", description = "Ya existe un usuario con el email indicado")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request){

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    @Operation(
        summary = "Desactivar un usuario del sistema (Baja lógica global)",
        description = """
            Realiza la baja lógica global de una cuenta de usuario en la plataforma cambiando su estado "enabled" a "false".
            
            **Políticas de Acceso:**
            - **Restricción Estricta:** Exclusivo para usuarios con "isSuperAdmin = true".
            - **Efecto Global:** Un usuario desactivado pierde el acceso a la plataforma.
            - Los administradores de congreso o el propio usuario no pueden ejecutar la baja global desde este endpoint.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuario eliminado correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado. Se requieren permisos de Súper Admin"),
        @ApiResponse(responseCode = "404", description = "No se encontró un usuario con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteUser(@PathVariable String code) {

        userService.deleteUser(code);

        return ResponseEntity.noContent().build();
    }


    ///----------------------------------------------------------PUT----------------------------------------------------------///
    @Operation(
        summary = "Actualizar datos globales de un usuario",
        description = """
            Modifica la información filiatoria y personal global de una cuenta de usuario (firstName, lastName, dni, country).
            
            **Políticas de Acceso y Control de Permisos:**
            - **Propietario de la cuenta (`isSelf`):** Un usuario autenticado siempre puede actualizar sus propios datos personales.
            - **Súper Admin:** Acceso concedido automáticamente de forma global.
            - **Otros usuarios / Admins de Congreso:** Denegado (`403 Forbidden`). La modificación de datos filiatorios personales es exclusiva del dueño de la cuenta o el Súper Admin del sistema.
            
            **Flujo de Ejecución:**
            1. **Extracción de Identidad:** Recupera el usuario autenticado desde el `SecurityContext`.
            2. **Búsqueda Target:** Localiza el usuario objetivo a partir del parámetro `{code}`.
            3. **Evaluación de Autorización:** Valida que sea el propio usuario o Súper Admin.
            4. **Persistencia y Respuesta:** Aplica los cambios mediante el mapper, persiste en BD y retorna el DTO actualizado.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No posee permisos para modificar este usuario (requiere ser el mismo usuario o su Admin de congreso)"),
        @ApiResponse(responseCode = "404", description = "No se encontró ningún usuario con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{code}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable String userCode, @Valid @RequestBody UpdateUserRequestDTO request) {

        return ResponseEntity.ok(userService.updateUser(userCode, request));
    }

    ///----------------------------------------------------------PATCH----------------------------------------------------------///
    @Operation(
        summary = "Actualizar el rol de un usuario en un congreso",
        description = """
            Modifica parcialmente la inscripción de un usuario actualizando únicamente el rol asignado en un congreso específico.
            
            **Políticas de Acceso:**
            - **Súper Admin:** Puede modificar cualquier rol en cualquier congreso.
            - **Admin de Congreso:** Requiere ser administrador activo sobre el `congressCode` enviado y que el usuario pertenezca al mismo.
            - **Otros roles:** Denegado (`403 Forbidden`).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "El rol enviado no es un valor válido del Enum"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No posee permisos de administración sobre el congreso especificado"),
        @ApiResponse(responseCode = "404", description = "El usuario no pertenece al congreso o el rol no existe")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("{congressCode}/{userCode}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable String congressCode, @PathVariable String userCode, @RequestBody RoleName newRole) {
        userService.updateUserRole(congressCode, userCode, newRole);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Cambiar o actualizar contraseña",
        description = "Permite al usuario autenticado cambiar su contraseña actual por una nueva. " +
                    "Este mismo endpoint atiende tanto el cambio voluntario desde el perfil como el primer ingreso obligatorio " +
                    "tras la asignación de una contraseña provisoria por un administrador (desactivando el indicador mustChangePassword)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "La contraseña actual es incorrecta o la nueva contraseña no cumple los requisitos"),
        @ApiResponse(responseCode = "401", description = "El usuario no se encuentra autenticado")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {

        return ResponseEntity.ok(userService.changePassword(request));
    }

    @Operation(
        summary = "Solicitar cambio de email",
        description = """
            Inicia el proceso de cambio de correo electrónico para el usuario autenticado.
            Valida la contraseña actual y envía un enlace de confirmación a la casilla de correo original.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitud iniciada. Enlace de confirmación enviado al correo actual"),
        @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o formato de email incorrecto"),
        @ApiResponse(responseCode = "401", description = "No autenticado o contraseña actual incorrecta"),
        @ApiResponse(responseCode = "409", description = "El nuevo email ya se encuentra registrado en el sistema")
    })
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/change-email")
    public ResponseEntity<MessageResponseDTO> changeEmail(@Valid @RequestBody ChangeEmailRequestDTO request) {

        return ResponseEntity.ok(userService.changeEmail(request));
    }
}

