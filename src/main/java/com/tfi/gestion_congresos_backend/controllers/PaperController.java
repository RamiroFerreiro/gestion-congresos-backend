package com.tfi.gestion_congresos_backend.controllers;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.PaperService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Operaciones relacionadas con la gestión de trabajos (Papers)")
public class PaperController {

        private final PaperService paperService;

        ///----------------------------------------------------------GETS----------------------------------------------------------///
        @Operation(
                summary = "Obtener trabajos asignados a un evaluador",
                description = "Obtiene la lista de Papers que tiene asignados un evaluador específico."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
                @ApiResponse(responseCode = "404", description = "No se encontró un evaluador con el ID especificado")
        })
        @GetMapping("/reviewer/{reviewerId}")
        public ResponseEntity<List<PaperResponseDTO>> getAssignedPapers(@PathVariable Long reviewerId) {
                return ResponseEntity.ok(paperService.getAssignedPapers(reviewerId));
        }

        @Operation(
                summary = "Obtener todos los trabajos de un congreso",
                description = "Obtiene la lista de todos los trabajos de un congreso, identificado por su código."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Trabajos obtenidos correctamente"),
                @ApiResponse(responseCode = "404", description = "No se encontró un congreso con el código especificado")
        })
        @GetMapping
        public ResponseEntity<List<PaperResponseDTO>> getPapersByCongress(@RequestParam String congressCode) {
                return ResponseEntity.ok(paperService.getPapersByCongressCode(congressCode));
        }

        @Operation(
                summary = "Obtener un trabajo por código",
                description = "Obtiene la información completa de un Paper a partir de su código de trackeo."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Trabajo obtenido correctamente"),
                @ApiResponse(responseCode = "404", description = "No se encontró un Paper con el código especificado")
        })
        @GetMapping("/{paperCode}")
        public ResponseEntity<PaperResponseDTO> getPaperByCode(@PathVariable String paperCode) {
                return ResponseEntity.ok(paperService.getPaperByCode(paperCode));
        }

        ///----------------------------------------------------------POSTS----------------------------------------------------------///
        @Operation(
                summary = "Crear un trabajo",
                description = "Crea un nuevo Paper, el cual nace en estado NOT_SUBMITTED. El código de trackeo se genera automáticamente. El primer autor de la lista se convierte en el autor principal (isMainAuthor), aceptado de inmediato; el resto queda como solicitudes pendientes."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Trabajo creado correctamente"),
                @ApiResponse(responseCode = "400", description = "Datos inválidos: congreso deshabilitado, área temática no válida, fecha fuera de ventana, tipo de trabajo de otro congreso, o el autor principal sin rol EXPOSITOR"),
                @ApiResponse(responseCode = "404", description = "No se encontró el congreso, el tipo de trabajo, o algún autor especificado")
        })
        @PostMapping
        public ResponseEntity<PaperResponseDTO> createPaper(@Valid @RequestBody PaperRequestDTO dto) {
                return ResponseEntity.status(HttpStatus.CREATED).body(paperService.createPaper(dto));
        }

        @Operation(
                summary = "Agregar un administrador como autor de un trabajo",
                description = "Endpoint exclusivo para agregar usuarios ADMINISTRATOR directamente, sin pasar por el flujo de solicitud/aceptación. Permitido únicamente en estado NOT_SUBMITTED."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Autor agregado correctamente"),
                @ApiResponse(responseCode = "400", description = "El Paper no está en NOT_SUBMITTED, o el usuario no es ADMINISTRATOR"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper o el usuario"),
                @ApiResponse(responseCode = "409", description = "El usuario ya tiene una relación con este Paper")
        })
        @PostMapping("/{paperCode}/authors/{userId}")
        public ResponseEntity<List<AuthorResponseDTO>> addAuthorToPaper(
                @PathVariable String paperCode, @PathVariable Long userId) {
                return ResponseEntity.status(HttpStatus.CREATED).body(paperService.addAuthorToPaper(paperCode, userId));
        }

        @Operation(
                summary = "Solicitar unirse como co-autor",
                description = "Un usuario con rol EXPOSITOR solicita unirse a un Paper existente. Genera una relación en estado PENDING, no cuenta contra el máximo de autores del congreso hasta ser aceptada."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Solicitud creada correctamente"),
                @ApiResponse(responseCode = "400", description = "El Paper no está en NOT_SUBMITTED, o el usuario no tiene rol EXPOSITOR en el congreso"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper o el usuario"),
                @ApiResponse(responseCode = "409", description = "El usuario ya tiene una relación (pendiente o aceptada) con este Paper")
        })
        @PostMapping("/{paperCode}/authors/requests/{userId}")
        public ResponseEntity<List<AuthorResponseDTO>> requestToJoinPaper(
                @PathVariable String paperCode, @PathVariable Long userId) {
                return ResponseEntity.status(HttpStatus.CREATED).body(paperService.requestToJoinPaper(paperCode, userId));
        }

        @Operation(
                summary = "Agregar una palabra clave a un trabajo",
                description = "Agrega una keyword a un Paper existente. Permitido en NOT_SUBMITTED y NEEDS_REVISION."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Palabra clave agregada correctamente"),
                @ApiResponse(responseCode = "400", description = "Estado inválido, duplicada, excede el máximo, o aparece en el título sin permitirlo"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper")
        })
        @PostMapping("/{paperCode}/keywords")
        public ResponseEntity<Set<String>> addKeywordToPaper(
                @PathVariable String paperCode, @RequestParam String keyword) {
                return ResponseEntity.status(HttpStatus.CREATED).body(paperService.addKeywordToPaper(paperCode, keyword));
        }

        ///----------------------------------------------------------DELETE----------------------------------------------------------///
        @Operation(
                summary = "Eliminar un autor de un trabajo",
                description = "Elimina la relación entre un usuario y un Paper ya ACEPTADO. Permitido únicamente en NOT_SUBMITTED. El autor principal nunca puede eliminarse."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Autor eliminado correctamente"),
                @ApiResponse(responseCode = "400", description = "El Paper no está en NOT_SUBMITTED, se intentó eliminar al autor principal, o el usuario está PENDING (usar reject)"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper, o el usuario no tiene relación con este Paper")
        })
        @DeleteMapping("/{paperCode}/authors/{userId}")
        public ResponseEntity<List<AuthorResponseDTO>> removeAuthorFromPaper(
                @PathVariable String paperCode, @PathVariable Long userId) {
                return ResponseEntity.ok(paperService.removeAuthorFromPaper(paperCode, userId));
        }

        @Operation(
                summary = "Eliminar una palabra clave de un trabajo",
                description = "Elimina una keyword de un Paper. Permitido en NOT_SUBMITTED y NEEDS_REVISION. No se puede eliminar si es la única restante."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Palabra clave eliminada correctamente"),
                @ApiResponse(responseCode = "400", description = "Estado inválido, o es la última palabra clave"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper o la palabra clave")
        })
        @DeleteMapping("/{paperCode}/keywords")
        public ResponseEntity<Set<String>> removeKeywordFromPaper(
                @PathVariable String paperCode, @RequestParam String keyword) {
                return ResponseEntity.ok(paperService.removeKeywordFromPaper(paperCode, keyword));
        }

        ///----------------------------------------------------------PATCH----------------------------------------------------------///
        @Operation(
                summary = "Asignar un evaluador a un trabajo",
                description = "Asigna un evaluador a un Paper. El evaluador debe pertenecer al mismo congreso que el Paper."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Evaluador asignado correctamente"),
                @ApiResponse(responseCode = "400", description = "El evaluador no pertenece al mismo congreso que el Paper"),
                @ApiResponse(responseCode = "404", description = "No se encontró el Paper o el evaluador"),
                @ApiResponse(responseCode = "409", description = "El Paper ya tiene un evaluador asignado, u otro conflicto de asignación")
        })
        @PatchMapping("/{paperCode}/reviewers/{reviewerId}")
        public ResponseEntity<MessageResponseDTO> assingReviewerToPaper(
                @PathVariable String paperCode, @PathVariable Long reviewerId) {
                return ResponseEntity.ok(paperService.assignReviewerToPaper(paperCode, reviewerId));
        }

        @Operation(
                summary = "Enviar un trabajo a revisión",
                description = "Transiciona el estado del Paper de NOT_SUBMITTED o NEEDS_REVISION a UNDER_EVALUATION."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Trabajo enviado a revisión correctamente"),
                @ApiResponse(responseCode = "400", description = "Estado inválido, sin autores, o no alcanza el mínimo de palabras clave"),
                @ApiResponse(responseCode = "404", description = "No se encontró un Paper con el código especificado")
        })
        @PatchMapping("/{paperCode}/submit")
        public ResponseEntity<PaperResponseDTO> submitPaper(@PathVariable String paperCode) {
                return ResponseEntity.ok(paperService.submitPaper(paperCode));
        }

        @Operation(
                summary = "Aceptar una solicitud de co-autoría",
                description = "El autor principal acepta la solicitud de un usuario, pasando su estado de PENDING a ACCEPTED."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Solicitud aceptada correctamente"),
                @ApiResponse(responseCode = "400", description = "El Paper ya no está en NOT_SUBMITTED, o se superó el máximo de autores"),
                @ApiResponse(responseCode = "404", description = "No existe una solicitud pendiente de ese usuario")
        })
        @PatchMapping("/{paperCode}/authors/requests/{userId}/accept")
        public ResponseEntity<List<AuthorResponseDTO>> acceptAuthorRequest(
                @PathVariable String paperCode, @PathVariable Long userId) {
                return ResponseEntity.ok(paperService.acceptAuthorRequest(paperCode, userId));
        }

        @Operation(
                summary = "Rechazar una solicitud de co-autoría",
                description = "El autor principal rechaza la solicitud de un usuario. La relación se elimina directamente, sin conservar historial."
        )
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Solicitud rechazada y eliminada correctamente"),
                @ApiResponse(responseCode = "400", description = "El Paper ya no está en NOT_SUBMITTED"),
                @ApiResponse(responseCode = "404", description = "No existe una solicitud pendiente de ese usuario")
        })
        @PatchMapping("/{paperCode}/authors/requests/{userId}/reject")
        public ResponseEntity<List<AuthorResponseDTO>> rejectAuthorRequest(
                @PathVariable String paperCode, @PathVariable Long userId) {
                return ResponseEntity.ok(paperService.rejectAuthorRequest(paperCode, userId));
        }
}