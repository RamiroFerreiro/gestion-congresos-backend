package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.EvaluationReleaseMode;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.ParticipantPaperActivityType;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.EvaluationMapper;
import com.tfi.gestion_congresos_backend.repository.EvaluationRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.EvaluationService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Set<PaperStatus> ALLOWED_RESULTS = EnumSet.of(PaperStatus.ACCEPTED, PaperStatus.REJECTED, PaperStatus.NEEDS_REVISION);

    private final EvaluationRepository evaluationRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final CongressService congressService;
    private final AuthorizationService authorizationService;
    private final EmailService emailService;
    private final CongressParticipantInstitutionValidator institutionValidator;
    private final EvaluationMapper evaluationMapper;
    private final SecurityEvaluator securityEvaluator;

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> getEvaluationsByPaperCode(String paperCode) {

        User user = authorizationService.getAuthenticatedUserEntity();
        Paper paper = findPaperOrThrow(paperCode);
        Congress congress = paper.getCongress();

        boolean isAdministrativeViewer = securityEvaluator.isSuperAdmin(user)
                || securityEvaluator.isAdminOfCongress(user, congress.getCode());
        boolean isAssignedReviewer = !isAdministrativeViewer
                && securityEvaluator.isAssignedReviewer(user, paper);
        boolean privileged = isAdministrativeViewer || isAssignedReviewer;
        boolean isAcceptedAuthor = !privileged
                && securityEvaluator.isAcceptedAuthor(user, paper);

        if (!privileged && !isAcceptedAuthor) {
            throw new UserDisabledException(
                    "No tenés permisos para ver las evaluaciones de este trabajo.");
        }
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congress.getCode());

        List<Evaluation> evaluations =
                evaluationRepository.findByPaper_CodeOrderByEvaluationDateDesc(paperCode);

        if (!privileged) {
            // Los autores solo ven las evaluaciones liberadas.
            evaluations = evaluations.stream()
                    .filter(Evaluation::isReleased)
                    .toList();
        }

        List<EvaluationResponseDTO> response = evaluations.stream()
                .map(evaluationMapper::toResponseDTO)
                .collect(Collectors.toList());

        response.forEach(this::clearEvaluatorIdentity);

        boolean canViewEvaluatorIdentity = isAdministrativeViewer
                || (isAcceptedAuthor && congress.isSingleBlind());

        if (canViewEvaluatorIdentity) {
            populateEvaluatorIdentity(evaluations, response);
        }

        return response;
    }

    @Override
    @Transactional
    public EvaluationResponseDTO createEvaluation(String paperCode, EvaluationRequestDTO requestDTO) {

        User user = authorizationService.getAuthenticatedUserEntity();
        Paper paper = findPaperOrThrow(paperCode);
        Congress congress = paper.getCongress();

        // 1. Autorización (antes de validar el estado interno del Paper)
        boolean isEligibleReviewer =
                securityEvaluator.isAssignedReviewer(user, paper)
                && user.isEnabled()
                && securityEvaluator.hasRoleInCongress(
                        user,
                        congress.getCode(),
                        RoleName.EVALUATOR
                );

        if (!isEligibleReviewer) {
            throw new UserDisabledException(
                    "Solo el evaluador asignado, con cuenta habilitada y rol EVALUATOR activo en el congreso, puede cargar una evaluación."
            );
        }
        
        // 2. Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congress.getCode());

        // 3. Estado del congreso y del Paper
        if (!congress.isEnabled()) {
            throw new ArgumentNotValidException("El congreso '" + congress.getName() + "' está deshabilitado.");
        }
        if (paper.getUserReviewer() == null) {
            throw new ArgumentNotValidException("El trabajo no tiene un evaluador asignado.");
        }
        if (paper.getStatus() != PaperStatus.UNDER_EVALUATION) {
            throw new ArgumentNotValidException(
                "Solo se puede evaluar un trabajo en estado UNDER_EVALUATION. Estado actual: " + paper.getStatus());
        }

        // 4. Conflicto de interés
        boolean isAuthor = securityEvaluator.isParticipantOfPaper(
                paper.getPaperId(),
                user.getUserId()
        );

        if (isAuthor) {
            throw new ArgumentNotValidException(
                    "Un autor del trabajo no puede evaluarlo."
            );
        }

        // 5. Una sola evaluación por versión
        if (evaluationRepository.existsByPaper_CodeAndEvaluatedVersion(paperCode, paper.getVersion())) {
            throw new ResourceAlreadyExistsException(
                "Ya existe una evaluación para la versión " + paper.getVersion() + " de este trabajo.");
        }

        // 6. Resultado y deadline
        validateResultAndDeadline(requestDTO, paper.getCongress());

        Evaluation evaluation = evaluationMapper.toEntity(requestDTO);
        evaluation.setPaper(paper);
        evaluation.setEvaluatedVersion(paper.getVersion());   // ANTES del resultado: el snapshot debe decir "1.0"
        evaluation.setEvaluationDate(DateUtils.now());
        evaluation.setNewStatus(requestDTO.getNewStatus());

        boolean immediate = congress.getEvaluationReleaseMode() == EvaluationReleaseMode.IMMEDIATE;
        evaluation.setReleased(immediate);
        evaluation.setReleasedAt(immediate ? DateUtils.now() : null);

        Evaluation saved = evaluationRepository.save(evaluation);
        if (immediate) {
            applyResult(saved);
            recordEvaluationReleasedActivity(saved.getPaper(), saved.getReleasedAt());
        }
        return evaluationMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public MessageResponseDTO releasePendingEvaluations(String congressCode) {

        User user = authorizationService.getAuthenticatedUserEntity();
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        if (!securityEvaluator.isSuperAdminOr(user, () -> securityEvaluator.isAdminOfCongress(user, congress.getCode()))) {
            throw new UserDisabledException("Solo un administrador del congreso puede liberar evaluaciones.");
        }
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congress.getCode());

        if (congress.getEvaluationReleaseMode() == EvaluationReleaseMode.IMMEDIATE) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' está en modo IMMEDIATE. " +
                "Las evaluaciones se liberan automáticamente al crearse, no hay nada pendiente por liberar manualmente.");
        }

        List<Evaluation> pending = evaluationRepository.findByPaper_Congress_CodeAndIsReleasedFalse(congressCode);
        LocalDateTime now = DateUtils.now();

        for (Evaluation evaluation : pending) {
            evaluation.setReleased(true);
            evaluation.setReleasedAt(now);
            applyResult(evaluation);
            recordEvaluationReleasedActivity(evaluation.getPaper(), now);
        }
        evaluationRepository.saveAll(pending);

        congress.setLastEvaluationReleaseDate(now);
        
        return new MessageResponseDTO(pending.size() + " evaluación(es) liberada(s) correctamente");
    }

    ///----------------------------------------------------------HELPERS----------------------------------------------------------///

    private Paper findPaperOrThrow(String paperCode) {
        return paperRepository.findByCode(paperCode)
                .orElseThrow(() -> new ResourceNotFoundException("Paper no encontrado con código: " + paperCode));
    }

    private void clearEvaluatorIdentity(EvaluationResponseDTO evaluationResponse) {
        evaluationResponse.setEvaluatorCode(null);
        evaluationResponse.setEvaluatorFullName(null);
    }

    private void populateEvaluatorIdentity(
            List<Evaluation> evaluations,
            List<EvaluationResponseDTO> response
    ) {
        Set<String> evaluatorCodes = evaluations.stream()
                .map(Evaluation::getCreatedBy)
                .filter(this::isHistoricalEvaluatorCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (evaluatorCodes.isEmpty()) {
            return;
        }

        Map<String, User> evaluatorsByCode = userRepository.findByCodeIn(evaluatorCodes)
                .stream()
                .collect(Collectors.toMap(User::getCode, Function.identity()));

        for (int index = 0; index < evaluations.size(); index++) {
            String evaluatorCode = evaluations.get(index).getCreatedBy();
            User evaluator = evaluatorsByCode.get(evaluatorCode);

            if (evaluator == null) {
                continue;
            }

            EvaluationResponseDTO evaluationResponse = response.get(index);
            evaluationResponse.setEvaluatorCode(evaluatorCode);
            evaluationResponse.setEvaluatorFullName(
                    evaluator.getFirstName() + " " + evaluator.getLastName()
            );
        }
    }

    private boolean isHistoricalEvaluatorCode(String evaluatorCode) {
        return evaluatorCode != null
                && !evaluatorCode.isBlank()
                && !"SYSTEM".equals(evaluatorCode);
    }

    private void validateResultAndDeadline(EvaluationRequestDTO dto, Congress congress) {
        if (!ALLOWED_RESULTS.contains(dto.getNewStatus())) {
            throw new ArgumentNotValidException(
                "El resultado de una evaluación solo puede ser " + ALLOWED_RESULTS + ".");
        }
        if (dto.getNewStatus() == PaperStatus.NEEDS_REVISION && dto.getNewDeadline() == null) {
            throw new ArgumentNotValidException("Una evaluación con resultado NEEDS_REVISION requiere una nueva fecha de cierre.");
        }
        if (dto.getNewStatus() == PaperStatus.NEEDS_REVISION
                && !dto.getNewDeadline().isBefore(congress.getStartDate())) {
            throw new ArgumentNotValidException(
                    "La fecha límite para una corrección debe ser anterior al inicio del evento."
            );
        }
        if (dto.getNewStatus() != PaperStatus.NEEDS_REVISION && dto.getNewDeadline() != null) {
            throw new ArgumentNotValidException("La nueva fecha de cierre solo aplica cuando el resultado es NEEDS_REVISION.");
        }
    }

    private void applyResult(Evaluation evaluation) {
        Paper paper = evaluation.getPaper();

        paper.setStatus(evaluation.getNewStatus());

        if (evaluation.getNewStatus() == PaperStatus.NEEDS_REVISION) {
            int major = Integer.parseInt(paper.getVersion().split("\\.")[0]);
            paper.setVersion((major + 1) + ".0");   // 1.0 -> 2.0
        }

        notifyAuthors(evaluation);
    }

    private void recordEvaluationReleasedActivity(Paper paper, LocalDateTime releasedAt) {
        paper.setLastActivityType(ParticipantPaperActivityType.EVALUATION_RELEASED);
        paper.setLastActivityAt(releasedAt);
    }

    private void notifyAuthors(Evaluation evaluation) {

        PaperStatus result = evaluation.getNewStatus();

        // Solo notificamos los resultados que tienen una plantilla asociada.
        if (result != PaperStatus.NEEDS_REVISION
                && result != PaperStatus.ACCEPTED
                && result != PaperStatus.REJECTED) {
            return;
        }

        Paper paper = evaluation.getPaper();

        // Obtener los autores que aceptaron participar del trabajo.
        List<PaperAuthor> acceptedAuthors = paper.getAuthors().stream()
                .filter(paperAuthor -> paperAuthor.getStatus() == PaperAuthorStatus.ACCEPTED)
                .toList();

        if (acceptedAuthors.isEmpty()) {
            return;
        }

        // Recuperar el código del evaluador desde la auditoría.
        String evaluatorCode = evaluation.getCreatedBy();

        if (evaluatorCode == null
                || evaluatorCode.isBlank()
                || "SYSTEM".equals(evaluatorCode)) {

            log.error(
                    "No se pueden enviar notificaciones PRE-M04/PRE-M05/PRE-M06 "
                            + "para el trabajo {} versión {}: no se identificó al evaluador mediante createdBy.",
                    paper.getCode(),
                    evaluation.getEvaluatedVersion()
            );

            return;
        }

        // Recuperar una única vez el usuario que realizó la evaluación.
        User evaluator = userRepository.findByCode(evaluatorCode).orElse(null);

        if (evaluator == null) {
            log.error(
                    "No se pueden enviar notificaciones PRE-M04/PRE-M05/PRE-M06 "
                            + "para el trabajo {} versión {}: no existe un usuario con código {}.",
                    paper.getCode(),
                    evaluation.getEvaluatedVersion(),
                    evaluatorCode
            );

            return;
        }

        // Identificar la plantilla que corresponde al resultado.
        String notificationCode = switch (result) {
            case NEEDS_REVISION -> "PRE-M04";
            case ACCEPTED -> "PRE-M05";
            case REJECTED -> "PRE-M06";
            default -> throw new IllegalStateException(
                    "No existe una notificación configurada para el resultado " + result
            );
        };

        // Enviar una notificación individual a cada autor aceptado.
        for (PaperAuthor paperAuthor : acceptedAuthors) {

            User recipient = paperAuthor.getAuthor();

            try {

                switch (result) {

                    case NEEDS_REVISION -> emailService.sendPaperEvaluationWithObservationsNotification(
                            recipient,
                            paper,
                            evaluation,
                            evaluator
                    );

                    case ACCEPTED -> emailService.sendPaperApprovalNotification(
                            recipient,
                            paper,
                            evaluation,
                            evaluator
                    );

                    case REJECTED -> emailService.sendPaperRejectionNotification(
                            recipient,
                            paper,
                            evaluation,
                            evaluator
                    );

                    default -> throw new IllegalStateException(
                            "No existe una notificación configurada para el resultado " + result
                    );
                }

            } catch (RuntimeException exception) {
                log.error(
                        "No se pudo enviar {} al autor {} del trabajo {}.",
                        notificationCode,
                        recipient.getEmail(),
                        paper.getCode(),
                        exception
                );
            }
        }
    }
    
    private void validateCurrentUserInstitution(String congressCode) {
        institutionValidator.validateAuthenticatedParticipantInstitution(
                congressCode
        );
    }
}
