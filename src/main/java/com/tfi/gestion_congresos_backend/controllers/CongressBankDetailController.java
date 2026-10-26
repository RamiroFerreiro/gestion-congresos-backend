package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;
import com.tfi.gestion_congresos_backend.services.CongressBankDetailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Congress Bank Details", description = "Gestión de datos bancarios para congresos de pago")
@RestController
@RequestMapping("/api/congress-bank-details")
@RequiredArgsConstructor
public class CongressBankDetailController {

    private final CongressBankDetailService bankDetailService;

    ///----------------------------------------------------------POSTS----------------------------------------------------------///
    
    @Operation(
        summary = "Registrar datos bancarios",
        description = """
            Permite registrar la información bancaria asociada a un congreso de pago.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - Solo puede ser ejecutado por un **Súper Administrador** o por un **Administrador del congreso** especificado.
            
            **Reglas de negocio:**
            - El congreso debe existir.
            - El congreso debe ser de pago (no se permite en congresos gratuitos).
            - Un congreso solo puede tener un registro de datos bancarios (debe ser único).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Datos bancarios creados y asociados exitosamente al congreso"),
        @ApiResponse(responseCode = "400", description = "Formato de solicitud inválido o intento de registrar datos bancarios en un congreso gratuito"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario no posee permisos de administración sobre el congreso"),
        @ApiResponse(responseCode = "404", description = "Congreso no encontrado"),
        @ApiResponse(responseCode = "409", description = "El congreso ya cuenta con datos bancarios registrados previamente"),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @PostMapping("/{congressCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CongressBankDetailResponseDTO> createBankDetail(
            @PathVariable String congressCode, @Valid @RequestBody CongressBankDetailRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(bankDetailService.create(congressCode, request));
    }

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Operation(
        summary = "Obtener datos bancarios de un congreso",
        description ="""
            Devuelve la información de la cuenta bancaria configurada para un congreso de pago.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - Permitido para **Súper Administradores** o **cualquier usuario inscripto** en el congreso (Administradores, Expositores, Oyentes, etc.).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos bancarios obtenidos correctamente"),
        @ApiResponse(responseCode = "401",  description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario no pertenece al congreso especificado"),
        @ApiResponse(responseCode = "404", description = "Congreso no encontrado o no posee datos bancarios registrados"),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @GetMapping("/{congressCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CongressBankDetailResponseDTO> getBankDetailByCongressId(@PathVariable String congressCode) {

        return ResponseEntity.ok(bankDetailService.getByCongressCode(congressCode));
    }


    @Operation(summary = "Verificar si un congreso posee datos bancarios cargados")
    @ApiResponses({
        @ApiResponse(responseCode = "404", description = "Congreso no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @GetMapping("/exists/{congressCode}")
    public ResponseEntity<Boolean> existsBankDetail(@PathVariable String congressCode) {
        return ResponseEntity.ok(bankDetailService.existsByCongressCode(congressCode));
    }

    ///----------------------------------------------------------PUT----------------------------------------------------------///

    @Operation(
        summary = "Actualizar datos bancarios",
        description = """
            Permite modificar la información bancaria previamente registrada para un congreso de pago.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - Permitido únicamente para **Súper Administradores** o **Administradores del congreso** correspondiente.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos bancarios actualizados correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos enviados no válidos"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario no posee permisos de administración sobre este congreso"),
        @ApiResponse(responseCode = "404", description = "No se encontraron datos bancarios registrados para el código de congreso proporcionado"                   ),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{congressCode}")
    public ResponseEntity<CongressBankDetailResponseDTO> updateBankDetail(
            @PathVariable String congressCode, @Valid CongressBankDetailRequestDTO request) {

        return ResponseEntity.ok(bankDetailService.update(congressCode, request));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///

    @Operation(
        summary = "Eliminar datos bancarios de un congreso",
        description = """
            Elimina la información bancaria asociada a un congreso de pago.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - Permitido únicamente para **Súper Administradores** o **Administradores del congreso** correspondiente.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Datos bancarios eliminados correctamente"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario no posee permisos de administración sobre este congreso"),
        @ApiResponse(responseCode = "404", description = "Congreso o datos bancarios no encontrados"),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @PreAuthorize("isAuthenticated()")                      
    @DeleteMapping("/{congressCode}")
    public ResponseEntity<Void> deleteBankDetail(@PathVariable String congressCode) {
        
        bankDetailService.delete(congressCode);
        return ResponseEntity.noContent().build();
    }

}