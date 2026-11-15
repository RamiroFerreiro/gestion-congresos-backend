package com.tfi.gestion_congresos_backend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookPaperDTO;
import com.tfi.gestion_congresos_backend.services.AbstractBookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Libro de Resúmenes", description = "Consulta y exportación del Libro de Resúmenes de un congreso")
@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class AbstractBookController {

    private static final MediaType WORD_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final AbstractBookService abstractBookService;

    @Operation(
            summary = "Consultar trabajos del Libro de Resúmenes",
            description = """
                    Devuelve los trabajos que cumplen las condiciones para incorporarse al Libro de Resúmenes.

                    **Condiciones de inclusión:**
                    - El trabajo debe estar académicamente aprobado (`ACCEPTED`).
                    - Si el congreso es pago, además debe tener el pago verificado.

                    **Campos:** código, título, área temática, resumen, palabras clave y autores ordenados
                    (nombre, apellido, institución y email).

                    **Restricciones de acceso:**
                    - Requiere autenticación.
                    - El usuario debe ser **Administrador** del congreso o **Súper Administrador**.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador del congreso"),
            @ApiResponse(responseCode = "404", description = "No se encontró un congreso con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{code}/abstract-book/papers")
    public ResponseEntity<List<AbstractBookPaperDTO>> getEligiblePapers(@PathVariable String code) {
        return ResponseEntity.ok(abstractBookService.getEligiblePapers(code));
    }

    @Operation(
            summary = "Exportar el Libro de Resúmenes en Word",
            description = """
                    Genera un documento Word (.docx) con la información estructurada de los trabajos
                    seleccionados, lista para armar el Libro de Resúmenes.

                    Cada trabajo incluye código, título, área temática, autores (en orden, con institución y email),
                    palabras clave y resumen.

                    Solo incluye trabajos académicamente aprobados (`ACCEPTED`). En congresos pagos,
                    el comprobante debe estar verificado (`APPROVED`). Los congresos gratuitos no requieren pago.

                    **Restricciones de acceso:**
                    - Requiere autenticación.
                    - El usuario debe ser **Administrador** del congreso o **Súper Administrador**.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento Word generado correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador del congreso"),
            @ApiResponse(responseCode = "404", description = "No se encontró un congreso con el código especificado")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{code}/abstract-book", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> generateAbstractBook(@PathVariable String code) {
        byte[] word = abstractBookService.generateAbstractBookWord(code);
        String filename = "libro-resumenes-" + code + ".docx";

        return ResponseEntity.ok()
                .contentType(WORD_MEDIA_TYPE)
                .contentLength(word.length)
                .headers(headers -> headers.setContentDisposition(ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()))
                .body(word);
    }
}
