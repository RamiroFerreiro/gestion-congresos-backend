package com.tfi.gestion_congresos_backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tfi.gestion_congresos_backend.dtos.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class CongressController {
	
	private final CongressService congressService;
	private final UserService userService;
	
	/// Traer congresos (Todos / Activos / Desactivados):
	@Operation(
            summary = "Obtener los congresos según el estado indicado",
            description = "Obtiene la lista de todos los congresos registrados en el sistema o los que estén activos o desactivos."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Congresos obtenidos correctamente"
    )
	@GetMapping
	public ResponseEntity<List<CongressResponseDTO>> getCongresses(@RequestParam(required = false) Boolean enabled) {
		
		if (enabled != null) {
			return ResponseEntity.ok(congressService.getCongressesByEnabled(enabled));
		}
		
		return ResponseEntity.ok(congressService.getAllCongresses());
	}
	
	/// Traer un congreso por ID:
	@Operation(
            summary = "Obtener un congreso por ID",
            description = "Obtiene la información de un congreso a partir de su identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el ID especificado"
            )
    })
	@GetMapping("/{congressId}")
	public ResponseEntity<CongressResponseDTO> getCongressById(@PathVariable Long congressId) {
	
		return ResponseEntity.ok(congressService.getCongressById(congressId));
	}
	
	/// Crear un congreso:
	@Operation(
            summary = "Crear un congreso",
            description = "Registra un nuevo congreso en el sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Congreso creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos. Razones posibles:\n" +
                            	  "- Las fechas de cada etapa deben ser futuras y la fecha de inicio debe ser anterior a la de fin.\n" +
                            	  "- La secuencia de etapas es inválida. La inscripción y presentación deben ocurrir antes del inicio del congreso."
            )
    })
	@PostMapping
	public ResponseEntity<CongressResponseDTO> createCongress(@Valid @RequestBody CongressRequestDTO request) {
		
		CongressResponseDTO created = congressService.createCongress(request);
        
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
	
	/// Desactivar congreso (baja lógica):
	@Operation(
            summary = "Desactivar un congreso por ID",
            description = "Establece como desactivado el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso desactivado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el ID especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El congreso ya se encontraba desactivado"
            )
    })
	@PatchMapping("/{congressId}/disable")
	public ResponseEntity<MessageResponseDTO> disableCongress(@PathVariable Long congressId) {
	    
		return ResponseEntity.ok(congressService.disableCongress(congressId));
	}

	/// Reactivar congreso:
	@Operation(
            summary = "Activar un congreso por ID",
            description = "Establece como activado el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso activado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el ID especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El congreso ya se encontraba activado"
            )
    })
	@PatchMapping("/{congressId}/enable")
	public ResponseEntity<MessageResponseDTO> enableCongress(@PathVariable Long congressId) {
	    
		return ResponseEntity.ok(congressService.enableCongress(congressId));
	}
	
	/// Actualizar un congreso:
	@Operation(
            summary = "Actualizar un congreso",
            description = "Modifica un congreso existente en el sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Congreso actualizado correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Los datos enviados no son válidos. Razones posibles:\n" +
                            	  "- Las fechas de cada etapa deben ser futuras y la fecha de inicio debe ser anterior a la de fin.\n" +
                            	  "- La secuencia de etapas es inválida. La inscripción y presentación deben ocurrir antes del inicio del congreso."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el ID especificado"
            ),
    })
	@PutMapping("/{congressId}")
	public ResponseEntity<CongressResponseDTO> updateCongress(@PathVariable Long congressId, @Valid @RequestBody CongressRequestDTO request) {
		
		return ResponseEntity.ok(congressService.updateCongress(congressId, request));
	}
	
	/// Obtener usuarios de un congreso con determinado rol:
	@Operation(
            summary = "Obtener los participantes de un congreso con determinado rol",
            description = "Obtiene la lista de todos los participantes registrados en el congreso que cuentan con el rol indicado."
    )
    @ApiResponses({
			@ApiResponse(
		            responseCode = "200",
		            description = "Participantes obtenidos correctamente"
		    ),
			@ApiResponse(
		            responseCode = "404",
		            description = "No se encontró un congreso con el ID especificado"
		    ),
	})
	@GetMapping("/{congressId}/participants")
	public ResponseEntity<List<UserResponseDTO>> getParticipantsByCongressAndRole(@PathVariable Long congressId, @RequestParam(required = false) RoleName role) {
		
		return ResponseEntity.ok(userService.getParticipantsByCongressAndRole(congressId, role));
	}
	
	/// Agregar un participante a un congreso:
	@Operation(
            summary = "Agregar un participante a un congreso",
            description = "Añade un participante a un congreso determinado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Participante agregado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Objeto inexistente. Razones posibles:\n" +
                            	  "- No se encontró un congreso con el ID especificado.\n" +
                            	  "- No se encontró un usuario con el ID especificado."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El participante ya estaba inscripto en el congreso"
            ),
    })
	@PostMapping("/{congressId}/participants/{participantId}")
	public ResponseEntity<MessageResponseDTO> addParticipantToCongress(@PathVariable Long congressId, @PathVariable Long participantId) {
		
		return ResponseEntity.ok(congressService.addParticipantToCongress(congressId, participantId));
	}
}

