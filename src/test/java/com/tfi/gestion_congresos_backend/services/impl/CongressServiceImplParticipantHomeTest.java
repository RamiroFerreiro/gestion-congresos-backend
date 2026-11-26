package com.tfi.gestion_congresos_backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.tfi.gestion_congresos_backend.services.CongressBankDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tfi.gestion_congresos_backend.dtos.congress.ParticipantHomeResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.dashboard.CongressDashboardResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.CongressDashboardMapper;
import com.tfi.gestion_congresos_backend.mapper.CongressMapper;
import com.tfi.gestion_congresos_backend.repository.CongressPaperTypeRepository;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.repository.PaperPaymentRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.repository.projection.ParticipantHomeAcademicMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.ParticipantHomePaymentMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.AcademicMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.FinancialMetricsProjection;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;

@ExtendWith(MockitoExtension.class)
class CongressServiceImplParticipantHomeTest {

    private static final String CONGRESS_CODE = "C-2026";

    @Mock private CongressRepository congressRepository;
    @Mock private CongressParticipantRepository congressParticipantRepository;
    @Mock private CongressPaperTypeRepository congressPaperTypeRepository;
    @Mock private CongressMapper congressMapper;
    @Mock private AuthorizationService authorizationService;
    @Mock private CongressBankDetailService congressBankDetailService;
    @Mock private CongressParticipantInstitutionValidator institutionValidator;
    @Mock private PaperRepository paperRepository;
    @Mock private PaperPaymentRepository paperPaymentRepository;
    @Mock private CongressDashboardMapper dashboardMapper;
    @Mock private EmailService emailService;
    @Mock private ParticipantHomeAcademicMetricsProjection academicMetrics;
    @Mock private ParticipantHomePaymentMetricsProjection paymentMetrics;

    private CongressServiceImpl service;
    private Congress congress;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CongressServiceImpl(congressRepository, congressParticipantRepository,
                congressPaperTypeRepository, congressMapper, authorizationService, congressBankDetailService,
                institutionValidator, paperRepository, paperPaymentRepository, dashboardMapper, emailService);

        congress = Congress.builder().code(CONGRESS_CODE).name("Congreso 2026").place("Lanús")
                .presentationEndDate(LocalDateTime.of(2026, 7, 31, 23, 59)).isFree(false).build();
        user = User.builder().userId(25L).code("USR-25").enabled(true).build();

        lenient().when(congressRepository.findByCodeWithThematicAreas(CONGRESS_CODE)).thenReturn(Optional.of(congress));
        lenient().when(authorizationService.getAuthenticatedUserEntity()).thenReturn(user);
    }

    @Test
    void returnsExpositorPersonalMetricsForAcceptedAuthorPapersInDifferentStates() {
        authorizeAs(RoleName.EXPOSITOR);
        when(academicMetrics.getTotalAssociatedPapers()).thenReturn(5L);
        when(academicMetrics.getDrafts()).thenReturn(1L);
        when(academicMetrics.getNeedsRevision()).thenReturn(1L);
        when(academicMetrics.getUnderEvaluation()).thenReturn(1L);
        when(paymentMetrics.getPaymentsWithoutValidReceipt()).thenReturn(2L);
        when(paymentMetrics.getPaymentsPendingApproval()).thenReturn(1L);
        when(paymentMetrics.getPaymentsVerified()).thenReturn(1L);
        stubMetrics();

        ParticipantHomeResponseDTO response = service.getParticipantHome(CONGRESS_CODE);

        assertEquals(5, response.getTotalAssociatedPapers());
        assertEquals(1, response.getDrafts());
        assertEquals(1, response.getNeedsRevision());
        assertEquals(1, response.getUnderEvaluation());
        assertEquals(2, response.getPaymentsWithoutValidReceipt());
        assertEquals(1, response.getPaymentsPendingApproval());
        assertEquals(1, response.getPaymentsVerified());
        assertEquals(congress.getPresentationEndDate(), response.getPresentationEndDate());
    }

    @Test
    void returnsZeroMetricsForListenerWithoutPapers() {
        authorizeAs(RoleName.LISTENER);
        stubMetrics();

        ParticipantHomeResponseDTO response = service.getParticipantHome(CONGRESS_CODE);

        assertEquals(0, response.getTotalAssociatedPapers());
        assertEquals(0, response.getPaymentsWithoutValidReceipt());
    }

    @Test
    void countsAcceptedCoauthorAcademicallyButNotAsResponsiblePayer() {
        authorizeAs(RoleName.EXPOSITOR);
        when(academicMetrics.getTotalAssociatedPapers()).thenReturn(1L);
        stubMetrics();

        ParticipantHomeResponseDTO response = service.getParticipantHome(CONGRESS_CODE);

        assertEquals(1, response.getTotalAssociatedPapers());
        assertEquals(0, response.getPaymentsWithoutValidReceipt());
        verify(paperRepository).getParticipantHomePaymentMetrics(
                CONGRESS_CODE, 25L, PaperAuthorStatus.ACCEPTED);
    }

    @Test
    void exposesAggregatedMissingPendingApprovedAndRejectedReceiptMetrics() {
        authorizeAs(RoleName.EXPOSITOR);
        when(paymentMetrics.getPaymentsWithoutValidReceipt()).thenReturn(2L); // missing and rejected
        when(paymentMetrics.getPaymentsPendingApproval()).thenReturn(1L);
        when(paymentMetrics.getPaymentsVerified()).thenReturn(1L);
        stubMetrics();

        ParticipantHomeResponseDTO response = service.getParticipantHome(CONGRESS_CODE);

        assertEquals(2, response.getPaymentsWithoutValidReceipt());
        assertEquals(1, response.getPaymentsPendingApproval());
        assertEquals(1, response.getPaymentsVerified());
    }

    @Test
    void returnsZeroPaymentMetricsForFreeCongressWithoutPaymentQuery() {
        congress.setFree(true);
        authorizeAs(RoleName.EXPOSITOR);
        when(academicMetrics.getTotalAssociatedPapers()).thenReturn(2L);
        when(paperRepository.getParticipantHomeAcademicMetrics(eq(CONGRESS_CODE), eq(25L), any()))
                .thenReturn(academicMetrics);

        ParticipantHomeResponseDTO response = service.getParticipantHome(CONGRESS_CODE);

        assertEquals(0, response.getPaymentsWithoutValidReceipt());
        assertEquals(0, response.getPaymentsPendingApproval());
        assertEquals(0, response.getPaymentsVerified());
        verify(paperRepository, never()).getParticipantHomePaymentMetrics(any(), any(), any());
    }

    @Test
    void rejectsNonEligibleRole() {
        authorizeAs(RoleName.EVALUATOR);

        assertThrows(AccessDeniedException.class, () -> service.getParticipantHome(CONGRESS_CODE));
    }

    @Test
    void returnsNotFoundForUnknownCongressBeforeParticipantAuthorization() {
        when(congressRepository.findByCodeWithThematicAreas("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getParticipantHome("UNKNOWN"));
        verify(institutionValidator, never()).requireAuthenticatedActiveParticipantWithInstitution("UNKNOWN");
    }

    @Test
    void keepsDashboardOnItsExistingAdministrativeAuthorizationFlow() {
        AcademicMetricsProjection dashboardAcademicMetrics = org.mockito.Mockito.mock(AcademicMetricsProjection.class);
        FinancialMetricsProjection dashboardFinancialMetrics = org.mockito.Mockito.mock(FinancialMetricsProjection.class);
        CongressDashboardResponseDTO dashboard = CongressDashboardResponseDTO.builder().congressCode(CONGRESS_CODE).build();
        when(paperRepository.getAcademicMetricsByCongressCode(CONGRESS_CODE)).thenReturn(dashboardAcademicMetrics);
        when(paperPaymentRepository.getFinancialMetricsByCongressCode(CONGRESS_CODE)).thenReturn(dashboardFinancialMetrics);
        when(dashboardMapper.toDashboardResponseDTO(congress, dashboardAcademicMetrics, dashboardFinancialMetrics))
                .thenReturn(dashboard);

        assertEquals(dashboard, service.getDashboardMetrics(CONGRESS_CODE));
        verify(authorizationService).validateCongressAdminPermission(user, CONGRESS_CODE);
    }

    private void authorizeAs(RoleName roleName) {
        CongressParticipant participant = CongressParticipant.builder()
                .role(Role.builder().name(roleName).build()).institution("UNLa").active(true).user(user).congress(congress)
                .build();
        when(institutionValidator.requireAuthenticatedActiveParticipantWithInstitution(CONGRESS_CODE))
                .thenReturn(participant);
    }

    private void stubMetrics() {
        when(paperRepository.getParticipantHomeAcademicMetrics(CONGRESS_CODE, 25L, PaperAuthorStatus.ACCEPTED))
                .thenReturn(academicMetrics);
        when(paperRepository.getParticipantHomePaymentMetrics(CONGRESS_CODE, 25L, PaperAuthorStatus.ACCEPTED))
                .thenReturn(paymentMetrics);
    }
}
