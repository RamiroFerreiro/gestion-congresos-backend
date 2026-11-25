package com.tfi.gestion_congresos_backend.services.validation;

import java.time.LocalDateTime;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

@Component
@RequiredArgsConstructor
public class CoauthorshipPolicy {
    private final CongressParticipantInstitutionValidator institutionValidator;
    private final AuthorizationService authorizationService;

    public CongressParticipant requireApplicant(String congressCode) {
        var user = authorizationService.getAuthenticatedUserEntity();
        var participant = institutionValidator.requireAuthenticatedActiveParticipantWithInstitution(congressCode);
        var role = participant.getRole().getName();
        if (user.isSuperAdmin() || (role != RoleName.LISTENER && role != RoleName.EXPOSITOR)) {
            throw new AccessDeniedException("Solo participantes LISTENER o EXPOSITOR pueden solicitar coautoría.");
        }
        return participant;
    }

    public static boolean isOpen(Congress congress, LocalDateTime now) {
        return congress.isEnabled() && congress.getPresentationStartDate() != null
                && congress.getPresentationEndDate() != null
                && !now.isBefore(congress.getPresentationStartDate())
                && !now.isAfter(congress.getPresentationEndDate());
    }

    public void requireOpen(Congress congress) {
        if (!isOpen(congress, DateUtils.now())) {
            throw new ArgumentNotValidException("La coautoría requiere un congreso habilitado y un período de presentación abierto.");
        }
    }
}
