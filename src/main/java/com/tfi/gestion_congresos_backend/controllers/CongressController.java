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
import com.tfi.gestion_congresos_backend.services.CongressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class CongressController {
	
	private final CongressService congressService;
	
	/// Traer congresos (Todos / Activos / Desactivados):
	@GetMapping
	public ResponseEntity<List<CongressResponseDTO>> getCongresses(@RequestParam(required = false) Boolean enabled) {
		
		if (enabled != null) {
			return ResponseEntity.ok(congressService.getCongressesByEnabled(enabled));
		}
		
		return ResponseEntity.ok(congressService.getAllCongresses());
	}
	
	/// Traer un congreso por ID:
	@GetMapping("/{congressId}")
	public ResponseEntity<CongressResponseDTO> getCongressById(@PathVariable Long congressId) {
	
		return ResponseEntity.ok(congressService.getCongressById(congressId));
	}
	
	/// Crear un congreso:
	@PostMapping
	public ResponseEntity<CongressResponseDTO> createCongress(@Valid @RequestBody CongressRequestDTO request) {
		
		CongressResponseDTO created = congressService.createCongress(request);
        
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
	
	/// Desactivar congreso (baja lógica):
	@PatchMapping("/{congressId}/disable")
	public ResponseEntity<Void> disableCongress(@PathVariable Long congressId) {
	    
		congressService.disableCongress(congressId);
	    
		return ResponseEntity.noContent().build();
	}

	/// Reactivar congreso:
	@PatchMapping("/{congressId}/enable")
	public ResponseEntity<Void> enableCongress(@PathVariable Long congressId) {
	    
		congressService.enableCongress(congressId);
	    
		return ResponseEntity.noContent().build();
	}
	
	/// Actualizar un congreso:
	@PutMapping("/{congressId}")
	public ResponseEntity<CongressResponseDTO> updateCongress(@PathVariable Long congressId, @Valid @RequestBody CongressRequestDTO request) {
		
		return ResponseEntity.ok(congressService.updateCongress(congressId, request));
	}
}
