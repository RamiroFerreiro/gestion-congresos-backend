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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
            description = "Asocia los datos bancarios para el cobro a un congreso de pago previamente creado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Datos bancarios creados correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos o el congreso es gratuito"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Congreso no encontrado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El congreso ya posee datos bancarios asociados"
            )
    })
    @PostMapping("/{congressId}")
    public ResponseEntity<CongressBankDetailResponseDTO> createBankDetail(
            @PathVariable Long congressId, @Valid @RequestBody CongressBankDetailRequestDTO request) {

        return ResponseEntity.ok(bankDetailService.create(congressId, request));
    }

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Operation(
            summary = "Obtener datos bancarios de un congreso",
            description = "Devuelve los datos bancarios registrados para un congreso específico por su ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Datos bancarios obtenidos correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Congreso o datos bancarios no encontrados"
            )
    })
    @GetMapping("/{congressId}")
    public ResponseEntity<CongressBankDetailResponseDTO> getBankDetailByCongressId(@PathVariable Long congressId) {

        return ResponseEntity.ok(bankDetailService.getByCongressId(congressId));
    }

    @Operation(summary = "Verificar si un congreso posee datos bancarios cargados")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "404",
                    description = "Congreso no encontrado"
            )
    })
    @GetMapping("/exists/{congressId}")
    public ResponseEntity<Boolean> existsBankDetail(@PathVariable Long congressId) {
        return ResponseEntity.ok(bankDetailService.existsByCongressId(congressId));
    }

    ///----------------------------------------------------------PUT----------------------------------------------------------///

    @Operation(
            summary = "Actualizar datos bancarios",
            description = "Modifica los datos bancarios asociados a un congreso existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Datos bancarios actualizados correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos enviados no válidos"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Congreso o datos bancarios no encontrados"
            )
    })
    @PutMapping("/{congressId}")
    public ResponseEntity<CongressBankDetailResponseDTO> updateBankDetail(
            @PathVariable Long congressId, @Valid CongressBankDetailRequestDTO request) {

        return ResponseEntity.ok(bankDetailService.update(congressId, request));
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///

    @Operation(
            summary = "Eliminar datos bancarios",
            description = "Elimina los datos bancarios asociados a un congreso existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Datos bancarios eliminados correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Congreso o datos bancarios no encontrados"
            )
    })
    @DeleteMapping("/{congressId}")
    public ResponseEntity<Void> deleteBankDetail(@PathVariable(name = "congressId") Long congressId) {
        
        bankDetailService.delete(congressId);
        return ResponseEntity.noContent().build();
    }

    
    
}