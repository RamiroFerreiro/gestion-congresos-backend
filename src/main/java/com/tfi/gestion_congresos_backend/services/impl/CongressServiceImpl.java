package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.dtos.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.CongressMapper;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.services.CongressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressServiceImpl implements CongressService {
	
	private final CongressRepository congressRepository;
	private final CongressMapper congressMapper;
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener todos los congresos con sus participantes:
	public List<CongressResponseDTO> getAllCongresses() {
		
		List<Congress> congresses = congressRepository.findAllCongressesWithParticipants();
		
		
		List<CongressResponseDTO> result = congresses.stream()
												.map(congressMapper::toCongressResponseDTO)
												.toList();
		return result;
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener todos los congresos activos/desactivados con sus participantes:
	public List<CongressResponseDTO> getCongressesByEnabled(boolean enabled) {
		
		List<Congress> congresses = congressRepository.findAllCongressesByEnabledWithParticipants(enabled);
		
		
		List<CongressResponseDTO> result = congresses.stream()
				.map(congressMapper::toCongressResponseDTO)
				.toList();
		return result;
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener un congreso con sus participantes por su ID:
	public CongressResponseDTO getCongressById(Long congressId) {
		 
		Congress congress = congressRepository.findByCongressId(congressId)
												.orElseThrow(() -> new ResourceNotFoundException("Congreso no encontrado con ID: " + congressId));;
		 
		CongressResponseDTO result = congressMapper.toCongressResponseDTO(congress);
		 
		return result;
	}
	 
	@Override
	@Transactional
	/// Crear un congreso:
	public CongressResponseDTO createCongress(CongressRequestDTO congressRequestDTO) {
		
		// Validación de fechas del congreso:
		validateCongressDates(congressRequestDTO);
		 
		Congress congress = congressMapper.toEntity(congressRequestDTO);
		
		// Activamos el congreso:
		congress.setEnabled(true);
		
		congress = congressRepository.save(congress);
		 
		CongressResponseDTO result = congressMapper.toCongressResponseDTO(congress);
		 
		return result;
	}
	
	@Override
	@Transactional
	/// Desactivar un congreso:
	public void disableCongress(Long congressId) {
	    changeCongressStatus(congressId, false);
	}

	@Override
	@Transactional
	/// Activar un congreso:
	public void enableCongress(Long congressId) {
	    changeCongressStatus(congressId, true);
	}
	 
	@Override
	@Transactional
	/// Actualizar un congreso:
	public CongressResponseDTO updateCongress(Long congressId, CongressRequestDTO congressRequestDTO) {
		 
		Congress congress = congressRepository.findByCongressId(congressId)
				 								.orElseThrow(() -> new ResourceNotFoundException("Congreso no encontrado con ID: " + congressId));
		 
		// Validación de fechas del congreso:
		validateCongressDates(congressRequestDTO);
		 
		congressMapper.updateCongressFromDto(congressRequestDTO, congress);
		 
		congress = congressRepository.save(congress);
		 
		CongressResponseDTO result = congressMapper.toCongressResponseDTO(congress);
		 
		return result;
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Determinar si existe un usuario de determinado rol en un congreso:
	public boolean existsByCongressIdAndUserIdAndRoleName(Long congressId, Long userId, RoleName role) {
		return congressRepository.existsByCongressIdAndUserIdAndRole(congressId, userId, role);
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Determinar si existe un congreso por su ID:
	public boolean existsById(Long congressId) {
		return congressRepository.existsById(congressId);
	}
	
	/// Cambiar de estado un congreso:
	private void changeCongressStatus(Long congressId, boolean targetStatus) {
	    Congress congress = congressRepository.findById(congressId)
	            				.orElseThrow(() -> new ResourceNotFoundException("Congreso no encontrado con ID: " + congressId));

	    congress.setEnabled(targetStatus);
	    
	    congressRepository.save(congress);
	}
	 
	/**
	 * Valida que una lista de fechas mantenga un orden cronológico estricto o no superpuesto.
	 * Retorna true si cada fecha es anterior (o igual) a la que le sigue.
	 */
	private boolean isChronologicalSequence(LocalDateTime... dates) {
	     
		for (int i = 0; i < dates.length - 1; i++) {
	         
			// Si la fecha actual es posterior a la siguiente, la secuencia es inválida
	        if (dates[i].isAfter(dates[i + 1])) {
	            return false;
	        }
	    }
	     
		return true;
	}
	 
	/**
	 * Valida que un rango individual de fechas sea coherente:
	 * - Ambas fechas deben ser futuras (posteriores a 'now').
	 * - La fecha de inicio debe ser estrictamente anterior a la fecha de fin.
	 */
	private boolean isValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
	     
		if (startDate == null || endDate == null) {
	        return false;
	    }
	     
	    LocalDateTime now = LocalDateTime.now();
	
	    boolean areInFuture = startDate.isAfter(now) && endDate.isAfter(now);
	    boolean isValidSequence = startDate.isBefore(endDate);
	
	    return areInFuture && isValidSequence;
	}

	/**
	 * Valida la totalidad de las fechas de un congreso (coherencia individual y orden global):
	 */
	private void validateCongressDates(CongressRequestDTO dto) {
		 
		// Extracción de fechas:
		LocalDateTime registrationStartDate = dto.getRegistrationStartDate();
		LocalDateTime registrationEndDate = dto.getRegistrationEndDate();
		LocalDateTime presentationStartDate = dto.getPresentationStartDate();
		LocalDateTime presentationEndDate = dto.getPresentationEndDate();
		LocalDateTime startDate = dto.getStartDate();
		LocalDateTime endDate = dto.getEndDate();
	
		// 1. Validar que cada par individual sea coherente (inicio < fin y futuras):
		boolean isRegistrationValid = isValidDateRange(registrationStartDate, registrationEndDate);
	    boolean isPresentationValid = isValidDateRange(presentationStartDate, presentationEndDate);
	    boolean isCongressValid = isValidDateRange(startDate, endDate);
	
	    if (!isRegistrationValid || !isPresentationValid || !isCongressValid) {
	        throw new ArgumentNotValidException(
	            "Las fechas de cada etapa deben ser futuras y la fecha de inicio debe ser anterior a la de fin."
	        );
	    }
	
	    // 2. Validar el orden cronológico entre las etapas del congreso:
	    // inicio inscripción <= fin inscripción <= inicio presentación <= fin presentación <= inicio congreso <= fin congreso
	    boolean isSequenceValid = isChronologicalSequence(
	    		registrationStartDate,
	    		registrationEndDate,
	    		presentationStartDate,
	    		presentationEndDate,
	    		startDate,
	    		endDate
	    		);
	
	    if (!isSequenceValid) {
	        throw new ArgumentNotValidException(
	            "La secuencia de etapas es inválida. La inscripción y presentación deben ocurrir antes del inicio del congreso."
	        );
	    }
	}
}
