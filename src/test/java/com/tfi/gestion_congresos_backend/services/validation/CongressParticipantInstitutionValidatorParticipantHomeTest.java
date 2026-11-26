package com.tfi.gestion_congresos_backend.services.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;

@ExtendWith(MockitoExtension.class)
class CongressParticipantInstitutionValidatorParticipantHomeTest {

    @Mock private AuthorizationService authorizationService;
    @Mock private CongressParticipantRepository participantRepository;
    @Mock private SecurityEvaluator securityEvaluator;

    private CongressParticipantInstitutionValidator validator;
    private User user;

    @BeforeEach
    void setUp() {
        validator = new CongressParticipantInstitutionValidator(
                authorizationService, participantRepository, securityEvaluator);
        user = User.builder().code("USR-1").enabled(true).build();
        when(authorizationService.getAuthenticatedUserEntity()).thenReturn(user);
    }

    @Test
    void rejectsInactiveParticipationWithForbidden() {
        when(participantRepository.findActiveParticipantByUserCodeAndCongressCode("USR-1", "C-1"))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> validator.requireAuthenticatedActiveParticipantWithInstitution("C-1"));
    }

    @Test
    void rejectsIncompleteInstitutionWithForbidden() {
        when(participantRepository.findActiveParticipantByUserCodeAndCongressCode("USR-1", "C-1"))
                .thenReturn(Optional.of(CongressParticipant.builder().institution(" ").active(true).build()));

        assertThrows(AccessDeniedException.class,
                () -> validator.requireAuthenticatedActiveParticipantWithInstitution("C-1"));
    }

    @Test
    void rejectsDisabledAuthenticatedUserWithForbidden() {
        user.setEnabled(false);

        assertThrows(UserDisabledException.class,
                () -> validator.requireAuthenticatedActiveParticipantWithInstitution("C-1"));
    }
}
