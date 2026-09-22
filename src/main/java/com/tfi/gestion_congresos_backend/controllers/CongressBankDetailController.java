package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    @SecurityRequirement(name = "") // Desactiva la exigencia de token en Swagger para esta prueba
    @PostMapping("/{congressId}")
    public ResponseEntity<CongressBankDetailResponseDTO> createBankDetail(
            @PathVariable Long congressId, @Valid @RequestBody CongressBankDetailRequestDTO request) {

        return ResponseEntity.ok(bankDetailService.create(congressId, request));
    }
}