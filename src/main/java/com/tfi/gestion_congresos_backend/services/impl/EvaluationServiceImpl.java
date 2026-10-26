package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.evaluation.EvaluationResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.EvaluationReleaseMode;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.EvaluationMapper;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.repository.EvaluationRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.EvaluationService;
import com.tfi.gestion_congresos_backend.services.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Set<PaperStatus> ALLOWED_RESULTS = EnumSet.of(PaperStatus.ACCEPTED, PaperStatus.REJECTED, PaperStatus.NEEDS_REVISION);

    private final EvaluationRepository evaluationRepository;
    private final PaperRepository paperRepository;
    private final CongressRepository congressRepository;
    private final EvaluationMapper evaluationMapper;
    private final SecurityEvaluator securityEvaluator;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> getEvaluationsByPaperCode(String paperCode) {

        User user = userService.getAuthenticatedUserEntity();
        Paper paper = findPaperOrThrow(paperCode);
        Congress congress = paper.getCongress();


        
        boolean privileged = securityEvaluator.isSuperAdminOr(user, () ->
                securityEvaluator.isAssignedReviewer(user, paper)
                || securityEvaluator.isAdminOfCongress(user, congress.getCode()));

        List<Evaluation> evaluations = evaluationRepository.findByPaper_CodeOrderByEvaluationDateDesc(paperCode);

        if (!privileged) {
            if (!securityEvaluator.isAcceptedAuthor(user, paper)) {
                throw new UserDisabledException("No tenés permisos para ver las evaluaciones de este trabajo.");
            }
            // Los autores solo ven lo ya liberado (CA2: en BATCH permanece oculta hasta el release)
            evaluations = evaluations.stream().filter(Evaluation::isReleased).toList();
        }

        return evaluations.stream().map(evaluationMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EvaluationResponseDTO createEvaluation(String paperCode, EvaluationRequestDTO requestDTO) {

        User user = userService.getAuthenticatedUserEntity();
        Paper paper = findPaperOrThrow(paperCode);
        Congress congress = paper.getCongress();

        // 1. Autorización (primero, para no revelar estado interno a quien no corresponde)
        if (!securityEvaluator.isSuperAdminOr(user, () -> securityEvaluator.isAssignedReviewer(user, paper))) {
            throw new UserDisabledException("Solo el evaluador asignado a este trabajo puede cargar una evaluación.");
        }

        // 2. Estado del congreso y del Paper
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

        // 3. Conflicto de interés
        boolean isAuthor = paper.getAuthors().stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(user.getUserId()));
        if (isAuthor) {
            throw new ArgumentNotValidException("Un autor del trabajo no puede evaluarlo.");
        }

        // 4. Una sola evaluación por versión
        if (evaluationRepository.existsByPaper_CodeAndEvaluatedVersion(paperCode, paper.getVersion())) {
            throw new ResourceAlreadyExistsException(
                "Ya existe una evaluación para la versión " + paper.getVersion() + " de este trabajo.");
        }

        // 5. Resultado y deadline
        validateResultAndDeadline(requestDTO);

        Evaluation evaluation = evaluationMapper.toEntity(requestDTO);
        evaluation.setPaper(paper);
        evaluation.setEvaluatedVersion(paper.getVersion());   // ANTES del resultado: el snapshot debe decir "1.0"
        evaluation.setEvaluationDate(LocalDateTime.now());
        evaluation.setNewStatus(requestDTO.getNewStatus());

        boolean immediate = congress.getEvaluationReleaseMode() == EvaluationReleaseMode.IMMEDIATE;
        evaluation.setReleased(immediate);
        evaluation.setReleasedAt(immediate ? LocalDateTime.now() : null);

        Evaluation saved = evaluationRepository.save(evaluation);
        if (immediate) {
            applyResult(saved);
        }
        return evaluationMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public MessageResponseDTO releasePendingEvaluations(String congressCode) {

        User user = userService.getAuthenticatedUserEntity();
        Congress congress = congressRepository.findByCode(congressCode)
                .orElseThrow(() -> new ResourceNotFoundException("Congreso no encontrado con código: " + congressCode));

        if (!securityEvaluator.isSuperAdminOr(user, () -> securityEvaluator.isAdminOfCongress(user, congress.getCode()))) {
            throw new UserDisabledException("Solo un administrador del congreso puede liberar evaluaciones.");
        }

        if (congress.getEvaluationReleaseMode() == EvaluationReleaseMode.IMMEDIATE) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' está en modo IMMEDIATE. " +
                "Las evaluaciones se liberan automáticamente al crearse, no hay nada pendiente por liberar manualmente.");
        }

        List<Evaluation> pending = evaluationRepository.findByPaper_Congress_CodeAndIsReleasedFalse(congressCode);
        LocalDateTime now = LocalDateTime.now();

        for (Evaluation evaluation : pending) {
            evaluation.setReleased(true);
            evaluation.setReleasedAt(now);
            applyResult(evaluation);
        }
        evaluationRepository.saveAll(pending);

        congress.setLastEvaluationReleaseDate(now);
        congressRepository.save(congress);

        // TODO: notificar a los postulantes afectados cuando exista un servicio de notificaciones.

        return new MessageResponseDTO(pending.size() + " evaluación(es) liberada(s) correctamente");
    }

    ///----------------------------------------------------------HELPERS----------------------------------------------------------///

    private Paper findPaperOrThrow(String paperCode) {
        return paperRepository.findByCode(paperCode)
                .orElseThrow(() -> new ResourceNotFoundException("Paper no encontrado con código: " + paperCode));
    }

    private void validateResultAndDeadline(EvaluationRequestDTO dto) {
        if (!ALLOWED_RESULTS.contains(dto.getNewStatus())) {
            throw new ArgumentNotValidException(
                "El resultado de una evaluación solo puede ser " + ALLOWED_RESULTS + ".");
        }
        if (dto.getNewStatus() == PaperStatus.NEEDS_REVISION && dto.getNewDeadline() == null) {
            throw new ArgumentNotValidException("Una evaluación con resultado NEEDS_REVISION requiere una nueva fecha de cierre.");
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



        notifyAuthors(paper, evaluation.getNewStatus());
    }

    // Por ahora solo loguea. Cuando exista el envío real de mails, se completa SOLO este método.
    private void notifyAuthors(Paper paper, PaperStatus result) {
        List<String> emails = paper.getAuthors().stream()
                .filter(pa -> pa.getStatus() == PaperAuthorStatus.ACCEPTED)
                .map(pa -> pa.getAuthor().getEmail())
                .toList();
        log.info("[NOTIFICACION] Trabajo {} -> {} (v{}) | destinatarios: {}",
                paper.getCode(), result, paper.getVersion(), emails);
    }
}