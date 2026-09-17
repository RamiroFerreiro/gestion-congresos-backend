package com.tfi.gestion_congresos_backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tfi.gestion_congresos_backend.dtos.RoleResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.services.RoleService;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(
            summary = "Obtener todos los roles",
            description = "Obtiene la lista de todos los roles registrados en el sistema."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuarios obtenidos correctamente"
    )
    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {

        return ResponseEntity.ok(roleService.getAllRoles());
    }
    

    @Operation(
        summary = "Obtener roles disponibles para registro",
        description = "Devuelve únicamente los roles válidos con los que un nuevo usuario puede registrarse (EXPOSITOR, LISTENER)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de roles obtenida correctamente"
    )
    @GetMapping("/register-roles")
    public ResponseEntity<List<RoleResponseDTO>> getRegisterableRoles() {
        return ResponseEntity.ok(roleService.getRegisterableRoles());
    }
}
