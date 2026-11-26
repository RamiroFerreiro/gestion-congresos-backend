package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerInboxPaperDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerInboxResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.ReviewerInboxActivity;
import com.tfi.gestion_congresos_backend.enums.ReviewerInboxStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.repository.EvaluationRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.ReviewerInboxService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewerInboxServiceImpl implements ReviewerInboxService {
    private final PaperRepository paperRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuthorizationService authorizationService;
    private final CongressService congressService;
    private final SecurityEvaluator securityEvaluator;
    private final CongressParticipantInstitutionValidator institutionValidator;

    @Override
    @Transactional(readOnly = true)
    public ReviewerInboxResponseDTO getMyInbox(String congressCode, ReviewerInboxStatus status) {
        User reviewer = authorizationService.getAuthenticatedUserEntity();
        if (!reviewer.isEnabled()
                || !securityEvaluator.hasRoleInCongress(reviewer, congressCode, RoleName.EVALUATOR)) {
            throw new AccessDeniedException("Solo los evaluadores activos del congreso pueden consultar su bandeja asignada.");
        }
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        List<Paper> assigned = paperRepository.findByCongress_CodeAndUserReviewer_Code(
                congressCode, reviewer.getCode());
        Map<Long, Evaluation> latestEvaluations = new HashMap<>();
        if (!assigned.isEmpty()) {
            evaluationRepository.findAssignedReviewerEvaluations(congressCode, reviewer.getCode())
                    .forEach(evaluation -> latestEvaluations.putIfAbsent(
                            evaluation.getPaper().getPaperId(), evaluation));
        }

        List<ReviewerInboxPaperDTO> papers = assigned.stream()
                .filter(paper -> paper.getStatus() != PaperStatus.NOT_SUBMITTED)
                .map(paper -> toInboxPaper(paper, latestEvaluations.get(paper.getPaperId())))
                .toList();

        List<ReviewerInboxPaperDTO> filtered = papers.stream()
                .filter(paper -> status == null || paper.getStatus() == status)
                .sorted(Comparator.comparing(ReviewerInboxPaperDTO::getStatus)
                        .thenComparing(ReviewerInboxPaperDTO::getLastActivityAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ReviewerInboxPaperDTO::getCode))
                .toList();

        return ReviewerInboxResponseDTO.builder()
                .congressCode(congress.getCode())
                .congressName(congress.getName())
                .totalAssigned(papers.size())
                .papers(filtered)
                .build();
    }

    private ReviewerInboxPaperDTO toInboxPaper(Paper paper, Evaluation latest) {
        ReviewerInboxStatus status = resolveStatus(paper, latest);
        ReviewerInboxActivity activity = ReviewerInboxActivity.ASSIGNED;
        LocalDateTime activityAt = paper.getReviewerAssignedAt();

        if (latest != null) {
            activity = ReviewerInboxActivity.EVALUATION_ISSUED;
            activityAt = latest.getEvaluationDate();
        }
        if (status == ReviewerInboxStatus.REVISION_PENDING) {
            activity = ReviewerInboxActivity.CORRECTION_RECEIVED;
            activityAt = paper.getSubmittedAt();
        }
        if (paper.getReviewerAssignedAt() != null
                && (activityAt == null || paper.getReviewerAssignedAt().isAfter(activityAt))) {
            activity = ReviewerInboxActivity.ASSIGNED;
            activityAt = paper.getReviewerAssignedAt();
        }

        return ReviewerInboxPaperDTO.builder()
                .code(paper.getCode())
                .title(paper.getTitle())
                .thematicArea(paper.getThematicArea())
                .version(paper.getVersion())
                .status(status)
                .lastActivity(activity)
                .lastActivityAt(activityAt)
                .build();
    }

    private ReviewerInboxStatus resolveStatus(Paper paper, Evaluation latest) {
        // En modo por lote el trabajo sigue UNDER_EVALUATION, aunque ya tiene dictamen.
        if (latest != null && !latest.isReleased()
                && paper.getVersion().equals(latest.getEvaluatedVersion())) {
            return ReviewerInboxStatus.PENDING_RELEASE;
        }
        return switch (paper.getStatus()) {
            case UNDER_EVALUATION -> latest != null
                    && latest.isReleased()
                    && latest.getNewStatus() == PaperStatus.NEEDS_REVISION
                    && !paper.getVersion().equals(latest.getEvaluatedVersion())
                    ? ReviewerInboxStatus.REVISION_PENDING : ReviewerInboxStatus.EVALUATION_PENDING;
            case NEEDS_REVISION -> ReviewerInboxStatus.NEEDS_REVISION;
            case ACCEPTED -> ReviewerInboxStatus.ACCEPTED;
            case REJECTED -> ReviewerInboxStatus.REJECTED;
            case NOT_SUBMITTED -> throw new IllegalStateException("Un borrador no pertenece a la bandeja asignada.");
        };
    }
}
