package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.Optional;

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
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
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
	public MessageResponseDTO  registerToCongress(String congressCode, CongressRegistrationRequestDTO requestDTO) {
		
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

		// Variable para construir el mensaje final de respuesta
    	String responseMessage;
		
		// 5. Buscar si existe una inscripción previa (activa o inactiva)
		Optional<CongressParticipant> participantOpt = participantRepository
			.findUserParticipantOfCongress(authenticatedUser.getCode(), congressCode);

		if (participantOpt.isPresent()) {
			CongressParticipant participant = participantOpt.get();

			// ESCENARIO A: Ya está inscripto y activo
			if (participant.isActive()) {
				throw new ArgumentNotValidException("Ya te encuentras inscripto en este congreso.");
			}

			// ESCENARIO B: Estaba dado de baja por un Administrador
			boolean selfDeleted = authenticatedUser.getCode().equals(participant.getDeletedBy());
			if (!selfDeleted) {
				throw new UserDisabledException("Tu inscripción fue cancelada por la administración del congreso. No es posible volver a inscribirse directamente.");
			}

			// ESCENARIO C: Estaba dado de baja por él mismo -> Reinscripción / Reactivación
			participant.setActive(true);
			participant.setInstitution(requestDTO.getInstitution());
			participant.setRegistrationDate(now);
			participant.setDeletedAt(null);
			participant.setDeletedBy(null);

			participantRepository.save(participant);
			responseMessage = "Te has vuelto a inscribir exitosamente al congreso '" + congress.getName() + "'.";

		} else {

			// PRIMERA INSCRIPCIÓN: No existe registro previo
			Role listenerRole = roleRepository.findByName(RoleName.LISTENER)
					.orElseThrow(() -> new ResourceNotFoundException("El rol LISTENER no se encuentra configurado en el sistema."));

			CongressParticipant newParticipant = CongressParticipant.builder()
					.congress(congress)
					.user(authenticatedUser)
					.role(listenerRole)
					.institution(requestDTO.getInstitution())
					.active(true)
					.registrationDate(now)
					.build();

			participantRepository.save(newParticipant);
			responseMessage = "Te has inscripto exitosamente al congreso '" + congress.getName() + "'.";
		}

		// Retorno de mensaje
		return MessageResponseDTO.builder()
				.message(responseMessage)
				.build();
	}

	@Transactional
    public MessageResponseDTO unregisterFromCongress(String targetUserCode, String congressCode) {

        // 1. Obtener el usuario que está realizando la acción
        User authenticatedUser = userService.getAuthenticatedUserEntity();

		// 2. Evaluar permisos: Solo superadmin, todos los usuarios consigo mismo
        boolean isSelf = authenticatedUser.getCode().equals(targetUserCode);
        boolean isSuperAdmin = securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode);

        if (!isSelf && !isSuperAdmin) {
            throw new UserDisabledException("No tienes permisos para modificar la información de este usuario.");
        }

        // 3. Buscar la participación activa en el congreso
        CongressParticipant participant = participantRepository
                .findUserActiveParticipantOfCongress(targetUserCode, congressCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No se encontró una inscripción activa para el usuario en el congreso: " + congressCode
                ));

		
        // 4. Aplicar la baja lógica registrando la auditoría
        participant.setActive(false);
        participant.setDeletedAt(LocalDateTime.now());
        participant.setDeletedBy(authenticatedUser.getCode()); // Se guarda el código de quien ejecutó la baja

        participantRepository.save(participant);

		// 5. Retornar mensaje de confirmación simple
		return MessageResponseDTO.builder()
				.message("El usuario ha salido exitosamente del congreso '" + participant.getCongress().getName() + "'.")
				.build();
    }
    
}
