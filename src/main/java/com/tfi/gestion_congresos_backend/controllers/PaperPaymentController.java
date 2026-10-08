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
import io.swagger.v3.oas.annotations.Parameter;
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
        summary = "Subir o re-subir comprobante de pago de un trabajo",
        description = """
            Permite adjuntar un archivo con el comprobante de transferencia o pago asociado a un trabajo de investigación.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - El usuario debe contar con el rol de **Expositor** en el congreso.
            - Debe ser el **Autor Principal (`main_author`)** del trabajo especificado.
            
            **Reglas de negocio:**
            - El archivo no puede estar vacío.
            - El trabajo debe existir y encontrarse en estado **ACEPTADO (`ACCEPTED`)**.
            - Si el pago ya fue previamente **APROBADO (`APPROVED`)**, no se permitirá subir un nuevo archivo.
            - Si existía un comprobante en estado pendiente o rechazado, el archivo anterior será reemplazado.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comprobante de pago subido exitosamente y registrado en estado PENDING_APPROVAL"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida: archivo no proporcionado/vacío, trabajo no aceptado o comprobante previamente APROBADO"),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado o token JWT inválido."),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: el usuario no es EXPOSITOR, no es el autor del trabajo o no es el autor principal."),
        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo asociado al código especificado o el usuario no existe."),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor al procesar o almacenar el archivo.")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/{paperCode}/payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaperPaymentResponseDTO> uploadPayment(
            @PathVariable String paperCode, 
            @Parameter(
                description = "Archivo del comprobante de pago (PDF, PNG, JPG)",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.uploadPayment(paperCode, file));
    }

    ///-------------------------------------------CONSULTAR ARCHIVO---------------------------------------------------------///
    
    @Operation(
        summary = "Visualizar / Descargar archivo comprobante de pago",
        description = "Devuelve el recurso físico (PDF o imagen) del comprobante de pago. " +
                    "Acceso permitido para usuarios ADMINISTRATOR/SUPERADMINISTRADOR (acceso global) o EXPOSITOR (solo si es integrante del trabajo)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Archivo recuperado exitosamente."),
        @ApiResponse(responseCode = "400", description = "Error al procesar la ruta física del archivo."),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado o token JWT inválido."),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: El rol no está autorizado (no es ADMIN ni EXPOSITOR) o el EXPOSITOR no pertenece al trabajo."),
        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo especificado, no existe comprobante registrado para el trabajo o el archivo físico no se encuentra en el servidor."),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @GetMapping(value = "/payments/file/{paperCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getPaymentFile(
            @Parameter(description = "Código del trabajo (ej: PAPER-2026-001)", required = true)
            @PathVariable String paperCode) {

        Resource fileResource = paymentService.getPaymentFile(paperCode);

        // Detectar el MediaType real del archivo (PDF, PNG, JPG, etc.)
        String contentType = null;
        try {
            contentType = java.nio.file.Files.probeContentType(fileResource.getFile().toPath());
        } catch (Exception e) {
            // Si no se logra determinar, se usa octet-stream por defecto
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }
    
    ///-------------------------------------------CONSULTAR INFORMACION DEL COMPROBANTE---------------------------------------------------------///

    @Operation(
        summary = "Obtener metadatos e información del comprobante de pago",
        description = "Devuelve los datos del comprobante, estado actual, montos, observaciones y datos de auditoría. " +
                    "Acceso permitido para usuarios ADMINISTRATOR (acceso global) o EXPOSITOR (solo si pertenece al trabajo)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metadatos del comprobante recuperados exitosamente."),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado o token JWT inválido."),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: Rol no autorizado o el EXPOSITOR no pertenece al trabajo."),
        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo o no existe un comprobante registrado para el mismo."),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @GetMapping("/payments/{paperCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaperPaymentResponseDTO> getPaymentDetails(@PathVariable String paperCode) {
        return ResponseEntity.ok(paymentService.getPaymentDetails(paperCode));
    }

    ///-------------------------------------------EVALUAR COMPROBANTE---------------------------------------------------------///

    @Operation(
        summary = "Aprobar o Rechazar un comprobante de pago",
        description = """
            Permite a un Administrador del congreso aprobar (`APPROVED`) o rechazar (`REJECTED`) el comprobante de pago enviado para un trabajo.
            
            **Restricciones de acceso:**
            - Requiere estar autenticado.
            - Permitido únicamente para **Súper Administradores** o **Administradores del congreso**.
            
            **Reglas de negocio:**
            - Un comprobante en estado `APPROVED` no puede volver a modificarse.
            - Si el estado enviado es `REJECTED`, la observación indicando el motivo es obligatoria.
            - Al aprobar el comprobante, el trabajo quedará marcado como pagado.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado del comprobante actualizado exitosamente."),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos u observación faltante al rechazar."),
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado o token JWT inválido."),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: Se requiere rol ADMINISTRATOR."),
        @ApiResponse(responseCode = "404", description = "No se encontró el trabajo especificado o no posee un comprobante cargado."),
        @ApiResponse(responseCode = "500", description = "Error no controlado del servidor.")
    })
    @PatchMapping("/payments/{paperCode}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaperPaymentResponseDTO> updatePaymentStatus(
            @PathVariable String paperCode, @Valid @RequestBody UpdatePaymentStatusDTO updateDTO) {

        return ResponseEntity.ok(paymentService.updatePaymentStatus(paperCode, updateDTO));
    }
}
