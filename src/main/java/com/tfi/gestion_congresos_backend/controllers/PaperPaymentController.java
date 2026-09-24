package com.tfi.gestion_congresos_backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Paper Payments", description = "Gestión de comprobantes de pago de trabajos")
@RestController
@RequestMapping("/api/papers/payments/{paperId}")
@RequiredArgsConstructor
public class PaperPaymentController {

    private final PaperPaymentService paymentService;

    @Operation(summary = "Subir comprobante de pago de un trabajo")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaperPaymentResponseDTO> uploadPayment(
        @PathVariable Long paperId, @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.uploadPayment(paperId, file));
    }
}
