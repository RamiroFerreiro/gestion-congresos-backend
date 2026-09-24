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

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Congress", description = "Operaciones relacionadas con la gestión de congresos")
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
            summary = "Obtener un congreso por código",
            description = "Obtiene la información de un congreso a partir de su código."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el código especificado"
            )
    })
	@GetMapping("/{code}")
	public ResponseEntity<CongressResponseDTO> getCongressByCode(@PathVariable String code) {
	
		return ResponseEntity.ok(congressService.getCongressDTOByCode(code));
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
            summary = "Desactivar un congreso por código",
            description = "Establece como desactivado el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso desactivado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el código especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El congreso ya se encontraba desactivado"
            )
    })
	@PatchMapping("/{code}/disable")
	public ResponseEntity<MessageResponseDTO> disableCongress(@PathVariable String code) {
	    
		return ResponseEntity.ok(congressService.disableCongress(code));
	}

	/// Reactivar congreso:
	@Operation(
            summary = "Activar un congreso por código",
            description = "Establece como activado el congreso."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Congreso activado correctamente"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No se encontró un congreso con el código especificado"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El congreso ya se encontraba activado"
            )
    })
	@PatchMapping("/{code}/enable")
	public ResponseEntity<MessageResponseDTO> enableCongress(@PathVariable String code) {
	    
		return ResponseEntity.ok(congressService.enableCongress(code));
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
                    description = "No se encontró un congreso con el código especificado"
            ),
    })
	@PutMapping("/{code}")
	public ResponseEntity<CongressResponseDTO> updateCongress(@PathVariable String code, @Valid @RequestBody CongressRequestDTO request) {
		
		return ResponseEntity.ok(congressService.updateCongress(code, request));
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
		            description = "No se encontró un congreso con el código especificado"
		    ),
	})
	@GetMapping("/{code}/participants")
	public ResponseEntity<List<UserResponseDTO>> getParticipantsByCongressAndRole(@PathVariable String code, @RequestParam(required = false) RoleName role) {
		
		return ResponseEntity.ok(userService.getParticipantsByCongressAndRole(code, role));
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
                            	  "- No se encontró un congreso con el código especificado.\n" +
                            	  "- No se encontró un usuario con el código especificado."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El participante ya estaba inscripto en el congreso"
            ),
    })
	@PostMapping("/{code}/participants/{participantCode}")
	public ResponseEntity<MessageResponseDTO> addParticipantToCongress(@PathVariable String code, @PathVariable String participantCode) {
		
		return ResponseEntity.ok(congressService.addParticipantToCongress(code, participantCode));
	}
}

