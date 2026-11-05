package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.EvaluationReleaseMode;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.CongressMapper;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressServiceImpl implements CongressService {
	
	private final CongressRepository congressRepository;
	private final CongressMapper congressMapper;
	private final AuthorizationService authorizationService;
	private final CongressParticipantInstitutionValidator institutionValidator;
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener todos los congresos con sus participantes:
	public List<CongressResponseDTO> getAllCongresses() {
		
		return congressRepository.findAllWithThematicAreas()
				.stream()
		        .map(congressMapper::toCongressResponseDTO)
		        .toList();
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener todos los congresos activos/desactivados con sus participantes:
	public List<CongressResponseDTO> getCongressesByEnabled(boolean enabled) {
		
		return congressRepository.findAllByEnabledWithThematicAreas(enabled)
				.stream()
		        .map(congressMapper::toCongressResponseDTO)
		        .toList();
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener la entidad de un congreso por su código:
	public Congress getCongressEntityByCode(String code) {

	    return congressRepository.findByCodeWithThematicAreas(code)
	            .orElseThrow(() ->
	                new ResourceNotFoundException(
	                    "Congreso no encontrado con código: " + code
	                )
	            );
	}
	
	@Override
	@Transactional(readOnly = true)
	/// Obtener el DTO de un congreso por su código:
	public CongressResponseDTO getCongressDTOByCode(String code) {

	    Congress congress = getCongressEntityByCode(code);

	    return congressMapper.toCongressResponseDTO(congress);
	}
	
	@Override
	@Transactional
	/// Crear un congreso:
	public CongressResponseDTO createCongress(CongressRequestDTO request) {
		
		// Validar permisos:
	    User authenticatedUser = authorizationService.getAuthenticatedUserEntity();
	    authorizationService.validateSuperAdminPermission(authenticatedUser);
		
		// Validación de fechas del congreso:
		validateCongressDatesForCreation(request);
		
		// Validación de rango de palabras clave límite:
		validateKeywordLimits(request);
		
		// Validar modo de publicación de evaluaciones y fecha definida:
		validateEvaluationReleaseConfiguration(request);
		 
		Congress congress = congressMapper.toEntity(request);
		
		// Generamos un código para el congreso:
		congress.setCode(generateCongressCode());
		
		// Activamos el congreso:
		congress.setEnabled(true);
		
		Congress savedCongress = congressRepository.save(congress);
		 
		return congressMapper.toCongressResponseDTO(savedCongress);
	}
	
	@Override
	@Transactional
	/// Actualizar un congreso:
	public CongressResponseDTO updateCongress(String code, CongressRequestDTO request) {
		
		// Obtener usuario autenticado:
		User authenticatedUser = authorizationService.getAuthenticatedUserEntity();
		
		// Validar permisos:
		authorizationService.validateCongressAdminPermission(authenticatedUser, code);
		
		// Validar institución del administrador:
		institutionValidator.validateAuthenticatedParticipantInstitution(code);
		
		// Obtener congreso:
		Congress congress = getCongressEntityByCode(code);
		 
		// Validación de fechas del congreso:
		validateCongressDatesForUpdate(request, congress);
		
		// Validación de rango de palabras clave límite:
		validateKeywordLimits(request);
		
		// Validar modo de publicación de evaluaciones y fecha definida:
		validateEvaluationReleaseConfiguration(request);
		 
		congressMapper.updateCongressFromDto(request, congress);
		 
		Congress updatedCongress = congressRepository.save(congress);
		 
		return congressMapper.toCongressResponseDTO(updatedCongress);
	}
	
	@Override
	@Transactional
	/// Desactivar un congreso:
	public MessageResponseDTO disableCongress(String code) {
	    
		changeCongressStatus(code, false);
	    
	    // Devolver mensaje de respuesta:
	 	return new MessageResponseDTO("Congreso desactivado con éxito");
	}

	@Override
	@Transactional
	/// Activar un congreso:
	public MessageResponseDTO enableCongress(String code) {
	    
		changeCongressStatus(code, true);
	    
	    // Devolver mensaje de respuesta:
	 	return new MessageResponseDTO("Congreso activado con éxito");
	}

	/// Cambiar de estado un congreso:
	private void changeCongressStatus(String code, boolean targetStatus) {
	    
		// Obtener usuario autenticado:
	    User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

	    // Validar permisos:
	    authorizationService.validateCongressAdminPermission(authenticatedUser, code);
	    
	    // Validar institución del administrador:
	    institutionValidator.validateAuthenticatedParticipantInstitution(code);
	    
	    // Obtener congreso:
	    Congress congress = getCongressEntityByCode(code);
	            				
	    if (congress.isEnabled() == targetStatus) {
	    	throw new ResourceAlreadyExistsException("El congreso ya se encontraba en ese estado");
	    }

	    congress.setEnabled(targetStatus);
	    
	    congressRepository.save(congress);
	}
	
	/// Validar rango de fechas:
	private boolean isValidDateRange(
	        LocalDateTime startDate,
	        LocalDateTime endDate
	) {
	    return startDate != null
	            && endDate != null
	            && startDate.isBefore(endDate);
	}
	
	/// Validar secuencia de fechas:
	private boolean isChronologicalSequence(LocalDateTime... dates) {

	    for (int i = 0; i < dates.length - 1; i++) {

	        if (dates[i].isAfter(dates[i + 1])) {
	            return false;
	        }
	    }

	    return true;
	}
	
	/// Validar que todas las fechas sean futuras:
	private boolean areAllDatesInFuture(LocalDateTime now, LocalDateTime... dates) {

	    for (LocalDateTime date : dates) {
	        if (date == null || !date.isAfter(now)) {
	            return false;
	        }
	    }

	    return true;
	}
	
	/// Validar si es una fecha futura o si no cambió:
	private boolean isFutureOrUnchanged(
	        LocalDateTime newDate,
	        LocalDateTime currentDate,
	        LocalDateTime now
	) {
	    return newDate != null
	            && (newDate.equals(currentDate) || newDate.isAfter(now));
	}

	/// Validar rangos y secuencia de fechas:
	private void validateDateRangesAndSequence(CongressRequestDTO dto) {

	    // 1. Validar que cada rango sea coherente (inicio < fin):

	    boolean isRegistrationValid = isValidDateRange(
	            dto.getRegistrationStartDate(),
	            dto.getRegistrationEndDate()
	    );

	    boolean isPresentationValid = isValidDateRange(
	            dto.getPresentationStartDate(),
	            dto.getPresentationEndDate()
	    );

	    boolean isCongressValid = isValidDateRange(
	            dto.getStartDate(),
	            dto.getEndDate()
	    );

	    if (!isRegistrationValid || !isPresentationValid || !isCongressValid) {
	        throw new ArgumentNotValidException(
	                "La fecha de inicio de cada etapa debe ser anterior a su fecha de fin."
	        );
	    }

	    // 2. Validar el orden cronológico entre las etapas:
	    // inicio inscripción <= fin inscripción
	    // <= inicio presentación <= fin presentación
	    // <= inicio congreso <= fin congreso.

	    boolean isSequenceValid = isChronologicalSequence(
	            dto.getRegistrationStartDate(),
	            dto.getRegistrationEndDate(),
	            dto.getPresentationStartDate(),
	            dto.getPresentationEndDate(),
	            dto.getStartDate(),
	            dto.getEndDate()
	    );

	    if (!isSequenceValid) {
	        throw new ArgumentNotValidException(
	                "La secuencia de etapas es inválida. La inscripción y presentación deben ocurrir antes del inicio del congreso."
	        );
	    }
	}
	
	/// Validar fechas al crear un congreso:
	private void validateCongressDatesForCreation(CongressRequestDTO dto) {

	    LocalDateTime now = LocalDateTime.now();

	    // Validar rangos y secuencia cronológica:
	    validateDateRangesAndSequence(dto);

	    // Comprobar que las seis fechas sean futuras:
	    boolean areDatesFuture = areAllDatesInFuture(
	            now,
	            dto.getRegistrationStartDate(),
	            dto.getRegistrationEndDate(),
	            dto.getPresentationStartDate(),
	            dto.getPresentationEndDate(),
	            dto.getStartDate(),
	            dto.getEndDate()
	    );

	    if (!areDatesFuture) {
	        throw new ArgumentNotValidException(
	                "Todas las fechas del congreso deben ser futuras al momento de su creación."
	        );
	    }

	    // Validar la fecha opcional de publicación de evaluaciones:
	    LocalDateTime scheduledDate = dto.getPlannedEvaluationReleaseDate();

	    if (scheduledDate != null && !scheduledDate.isAfter(now)) {
	        throw new ArgumentNotValidException(
	                "La fecha de publicación programada de evaluaciones debe ser futura."
	        );
	    }
	}
	
	/// Validar fechas al modificar un congreso:
	private void validateCongressDatesForUpdate(
	        CongressRequestDTO dto,
	        Congress congress
	) {

	    LocalDateTime now = LocalDateTime.now();

	    // Validar rangos y secuencia cronológica:
	    validateDateRangesAndSequence(dto);

	    // Comprobar que las fechas modificadas sean futuras
	    // o que conserven exactamente el valor anterior:
	    boolean areDatesValid =
	            isFutureOrUnchanged(dto.getRegistrationStartDate(), congress.getRegistrationStartDate(), now)
	            && isFutureOrUnchanged(dto.getRegistrationEndDate(), congress.getRegistrationEndDate(), now)
	            && isFutureOrUnchanged(dto.getPresentationStartDate(), congress.getPresentationStartDate(), now)
	            && isFutureOrUnchanged(dto.getPresentationEndDate(), congress.getPresentationEndDate(), now)
	            && isFutureOrUnchanged(dto.getStartDate(), congress.getStartDate(), now)
	            && isFutureOrUnchanged(dto.getEndDate(), congress.getEndDate(), now);

	    if (!areDatesValid) {
	        throw new ArgumentNotValidException(
	                "Las fechas que se modifiquen deben ser futuras. Las fechas pasadas solo pueden conservarse sin cambios."
	        );
	    }

	    // Validar la fecha opcional de publicación de evaluaciones:
	    LocalDateTime newScheduledDate = dto.getPlannedEvaluationReleaseDate();
	    LocalDateTime currentScheduledDate = congress.getPlannedEvaluationReleaseDate();

	    if (newScheduledDate != null
	            && !isFutureOrUnchanged(newScheduledDate, currentScheduledDate, now)) {

	        throw new ArgumentNotValidException(
	                "La fecha de publicación programada debe ser futura si se modifica."
	        );
	    }
	}
	
	/// Genera un código de congreso no duplicado:
	private String generateCongressCode() {
		String code;

	    do {
	        code = "%06d".formatted(
	            ThreadLocalRandom.current().nextInt(1_000_000)
	        );

	    } while (congressRepository.existsByCode(code));

	    return code;
    }
	
	/// Validar rango de palabras clave límite:
	private void validateKeywordLimits(CongressRequestDTO dto) {

	    Integer min = dto.getMinKeywords();
	    Integer max = dto.getMaxKeywords();

	    if (min != null && max != null && min > max) {
	        throw new ArgumentNotValidException(
	                "La cantidad mínima de palabras clave no puede superar la cantidad máxima."
	        );
	    }
	}
	
	/// Validar modo de publicación de evaluaciones y fecha definida:
	private void validateEvaluationReleaseConfiguration(CongressRequestDTO dto) {

	    EvaluationReleaseMode mode = dto.getEvaluationReleaseMode();
	    LocalDateTime scheduledDate = dto.getPlannedEvaluationReleaseDate();

	    if (mode == EvaluationReleaseMode.BATCH && scheduledDate == null) {
	        throw new ArgumentNotValidException(
	                "Debe especificar una fecha de publicación para el modo BATCH."
	        );
	    }

	    if (mode == EvaluationReleaseMode.IMMEDIATE && scheduledDate != null) {
	        throw new ArgumentNotValidException(
	                "No debe especificar una fecha de publicación para el modo IMMEDIATE."
	        );
	    }
	}
}
