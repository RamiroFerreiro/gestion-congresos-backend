package com.tfi.gestion_congresos_backend.controllers;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.ErrorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.UpdatePaymentStatusDTO;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Paper Payments", description = "Gestión de comprobantes de pago de trabajos")
@RestController
@RequestMapping("/api/papers") // Ruta base del recurso principal
@RequiredArgsConstructor
public class PaperPaymentController {

    private final PaperPaymentService paymentService;


    ///-------------------------------------------CARGAR/ACTUALIZAR COMPROBANTE---------------------------------------------------------///
    
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
            description = "Petición inválida, el trabajo no se encuentra en estado ACEPTADO o el comprobante ya fue APROBADO.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Usuario no autenticado o token JWT inválido.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acceso denegado: el usuario no es EXPOSITOR, no es el autor del trabajo o no es el autor principal.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró el trabajo asociado al código especificado o el usuario no existe.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor al procesar o almacenar el archivo.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    @PreAuthorize("hasRole('EXPOSITOR')")
    @PostMapping(value = "/payments/{paperCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaperPaymentResponseDTO> uploadPayment(
            @PathVariable String paperCode, @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.uploadPayment(paperCode, file));
    }

    ///-------------------------------------------CONSULTAR ARCHIVO---------------------------------------------------------///
    
    @Operation(
        summary = "Visualizar / Descargar archivo comprobante de pago",
        description = "Devuelve el recurso físico (PDF o imagen) del comprobante de pago. " +
                    "Acceso permitido para usuarios ADMINISTRATOR (acceso global) o EXPOSITOR (solo si es integrante del trabajo).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Archivo recuperado exitosamente.",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error al procesar la ruta física del archivo.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Usuario no autenticado o token JWT inválido.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acceso denegado: El rol no está autorizado (no es ADMIN ni EXPOSITOR) o el EXPOSITOR no pertenece al trabajo.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró el trabajo especificado, no existe comprobante registrado para el trabajo o el archivo físico no se encuentra en el servidor.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error no controlado del servidor.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    @GetMapping(value = "/payments/file/{paperCode}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'EXPOSITOR')")   
    public ResponseEntity<byte[]> getPaymentFile(@PathVariable String paperCode) throws IOException {

        Resource resource = paymentService.getPaymentFile(paperCode);

        // Determinar MediaType dinámicamente con fallback por extensión
        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ignored) {}

        if (contentType == null) {
            String filename = resource.getFilename();
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if (filename != null && (filename.toLowerCase().endsWith(".png"))) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (filename != null && (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg"))) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        }

        byte[] fileBytes = resource.getInputStream().readAllBytes();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(fileBytes);
    }

    ///-------------------------------------------CONSULTAR INFORMACION DEL COMPROBANTE---------------------------------------------------------///

    @Operation(
        summary = "Obtener metadatos e información del comprobante de pago",
        description = "Devuelve los datos del comprobante, estado actual, montos, observaciones y datos de auditoría. " +
                    "Acceso permitido para usuarios ADMINISTRATOR (acceso global) o EXPOSITOR (solo si pertenece al trabajo).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Metadatos del comprobante recuperados exitosamente."
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Usuario no autenticado o token JWT inválido.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Acceso denegado: Rol no autorizado o el EXPOSITOR no pertenece al trabajo.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No se encontró el trabajo o no existe un comprobante registrado para el mismo.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Error no controlado del servidor.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    @GetMapping("/payments/{paperCode}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'EXPOSITOR')")
    public ResponseEntity<PaperPaymentResponseDTO> getPaymentDetails(@PathVariable String paperCode) {
        return ResponseEntity.ok(paymentService.getPaymentDetails(paperCode));
    }

    ///-------------------------------------------EVALUAR COMPROBANTE---------------------------------------------------------///

    @Operation(
        summary = "Aprobar o Rechazar un comprobante de pago",
        description = "Permite a un ADMINISTRATOR cambiar el estado del pago (APPROVED / REJECTED) y agregar observaciones. " +
                    "Si el estado se marca como REJECTED, el campo observaciones es obligatorio.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Estado del comprobante actualizado exitosamente."
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Datos de entrada inválidos u observación faltante al rechazar.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Usuario no autenticado o token JWT inválido.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Acceso denegado: Se requiere rol ADMINISTRATOR.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No se encontró el trabajo especificado o no posee un comprobante cargado.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Error no controlado del servidor.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    @PatchMapping("/payments/{paperCode}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<PaperPaymentResponseDTO> updatePaymentStatus(
            @PathVariable String paperCode, @Valid @RequestBody UpdatePaymentStatusDTO updateDTO) {

        return ResponseEntity.ok(paymentService.updatePaymentStatus(paperCode, updateDTO));
    }
}
