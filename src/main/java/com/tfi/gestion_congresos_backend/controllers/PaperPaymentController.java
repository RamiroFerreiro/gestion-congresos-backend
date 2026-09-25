package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.ErrorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Paper Payments", description = "Gestión de comprobantes de pago de trabajos")
@RestController
@RequestMapping("/api/papers") // Ruta base del recurso principal
@RequiredArgsConstructor
public class PaperPaymentController {

    private final PaperPaymentService paymentService;

    @Operation(
        summary = "Subir comprobante de pago de un trabajo",
        description = "Permite al autor principal de un trabajo (con rol EXPOSITOR) subir o actualizar el comprobante de pago en formato PDF u otra imagen autorizada.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Comprobante de pago subido exitosamente."
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Petición inválida o el trabajo no se encuentra en estado ACEPTADO."
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Usuario no autenticado o token JWT inválido."
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acceso denegado: el usuario no es EXPOSITOR, no es el autor del trabajo o no es el autor principal."
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró el trabajo asociado al código especificado o el usuario no existe."
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor al procesar o almacenar el archivo."
        )
    })
    @PostMapping(value = "/payments/{paperCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaperPaymentResponseDTO> uploadPayment(
            @PathVariable String paperCode, @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.uploadPayment(paperCode, file));
    }
}
