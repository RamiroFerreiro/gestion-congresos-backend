package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.CongressRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressParticipantServiceImpl implements CongressParticipantService {

    private final UserService userService;
	private final SecurityEvaluator securityEvaluator;
	private final RoleRepository roleRepository;
	private final CongressParticipantRepository participantRepository;
    private final CongressService congressService;

    @Override
    @Transactional
	public MessageResponseDTO registerToCongress(String congressCode, CongressRegistrationRequestDTO requestDTO) {
		
		// 1. Obtener usuario autenticado
		User authenticatedUser = userService.getAuthenticatedUserEntity();

		// 2. Obtener el Congreso por su código (Lanza ResourceNotFoundException 404 si no existe)
		Congress congress = congressService.getCongressByCode(congressCode);

		// 3. Regla Fail-Fast: Validar que el congreso esté activo/habilitado
		if (!congress.isEnabled()) {
			throw new ArgumentNotValidException("El congreso no se encuentra habilitado para recibir inscripciones.");
		}

		// 4. Regla Fail-Fast: Validar período de inscripción
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(congress.getRegistrationStartDate()) || now.isAfter(congress.getRegistrationEndDate())) {
			throw new ArgumentNotValidException("El período de inscripción para este congreso no se encuentra activo.");
		}

		// 5. Regla Fail-Fast: Validar que el usuario no esté ya inscripto
		if (securityEvaluator.isParticipantOfCongress(authenticatedUser, congressCode)) {
			throw new ArgumentNotValidException("Ya te encuentras inscripto en este congreso.");
		}

		// 6. Obtener el rol por defecto LISTENER
		Role listenerRole = roleRepository.findByName(RoleName.LISTENER)
				.orElseThrow(() -> new ResourceNotFoundException("El rol LISTENER no se encuentra configurado en el sistema."));

		// 7. Determinar la institución 
		String institution = requestDTO.getInstitution();

		// 8. Crear la entidad de participación
		CongressParticipant participant = CongressParticipant.builder()
				.congress(congress)
				.user(authenticatedUser)
				.role(listenerRole)
				.institution(institution)
				.active(true)
				.registrationDate(now)
				.build();

		// 9. Persistir en base de datos
		participantRepository.save(participant);

		// 10. Retornar mensaje de confirmación simple
		return MessageResponseDTO.builder()
				.message("Te has inscripto exitosamente al congreso '" + congress.getName() + "'.")
				.build();
	}
    
}
