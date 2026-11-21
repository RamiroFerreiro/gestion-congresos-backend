package com.tfi.gestion_congresos_backend.services.validation;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CongressParticipantInstitutionValidator {

    private final AuthorizationService authorizationService;
    private final CongressParticipantRepository congressParticipantRepository;
    private final SecurityEvaluator securityEvaluator;

    @Transactional(readOnly = true)
    public void validateAuthenticatedParticipantInstitution(String congressCode) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Los súper administradores no necesitan una participación en el congreso
        if (securityEvaluator.isSuperAdmin(authenticatedUser)) {
            return;
        }

        // 3. Buscar su participación activa en el congreso
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        authenticatedUser.getCode(),
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa del usuario autenticado "
                        + "en el congreso: " + congressCode
                ));

        // 4. Verificar que haya completado su institución
        if (participant.getInstitution() == null
                || participant.getInstitution().isBlank()) {

            throw new AccessDeniedException(
                    "Debés completar tu institución antes de acceder a las funcionalidades del congreso."
            );
        }
    }

    /**
     * Validates the participant requirements for personal participant-only resources.
     * Unlike the historical validator above, a missing/inactive participation is an
     * authorization failure and super administrators are not exempt: these resources
     * are available exclusively to actual congress participants.
     */
    @Transactional(readOnly = true)
    public CongressParticipant requireAuthenticatedActiveParticipantWithInstitution(String congressCode) {
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        if (!authenticatedUser.isEnabled()) {
            throw new UserDisabledException("El usuario autenticado se encuentra deshabilitado.");
        }

        CongressParticipant participant = congressParticipantRepository
                .findActiveParticipantByUserCodeAndCongressCode(authenticatedUser.getCode(), congressCode)
                .orElseThrow(() -> new AccessDeniedException(
                        "El usuario no posee una participaciÃ³n activa en el congreso."
                ));

        if (participant.getInstitution() == null || participant.getInstitution().isBlank()) {
            throw new AccessDeniedException(
                    "DebÃ©s completar tu instituciÃ³n antes de acceder a las funcionalidades del congreso."
            );
        }

        return participant;
    }
}
