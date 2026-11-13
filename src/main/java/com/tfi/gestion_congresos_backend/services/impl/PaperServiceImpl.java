package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.paper.*;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.*;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperMapper;
import com.tfi.gestion_congresos_backend.repository.CongressPaperTypeRepository;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.EvaluationRepository;
import com.tfi.gestion_congresos_backend.repository.PaperAuthorRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final CongressParticipantRepository congressParticipantRepository;
    private final CongressPaperTypeRepository congressPaperTypeRepository;
    private final EvaluationRepository evaluationRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final CongressParticipantService congressParticipantService;
    private final EmailService emailService;
    private final CongressParticipantInstitutionValidator institutionValidator;
    private final SecurityEvaluator securityEvaluator;

    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperSummaryResponseDTO> getAssignedPapers(
            String congressCode,
            String reviewerCode) {

        // Validar congreso
        congressService.getCongressEntityByCode(congressCode);

        // Obtener usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // Validar permisos del usuario solicitante
        boolean isCongressAdmin =
                securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode);

        boolean isSelf =
                Objects.equals(authenticatedUser.getCode(), reviewerCode);

        if (!isCongressAdmin && !isSelf) {
            throw new AccessDeniedException(
                    "No tienes permisos para consultar los trabajos asignados a este evaluador."
            );
        }
        
        // Validar que el usuario solicitante tenga la institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // Buscar evaluador
        User reviewer = userService.getUserByUserCode(reviewerCode);

        // Validar que tenga el rol de evaluador en el congreso
        if (!securityEvaluator.hasRoleInCongress(
                reviewer,
                congressCode,
                RoleName.EVALUATOR)) {

            throw new ResourceNotFoundException(
                    "No se encontró un evaluador activo con el código: "
                    + reviewerCode + " en el congreso: " + congressCode
            );
        }

        // Obtener trabajos asignados al evaluador
        List<Paper> assignedPapers =
                paperRepository.findByCongress_CodeAndUserReviewer_Code(
                        congressCode,
                        reviewerCode
                );

        return paperMapper.toPaperSummaryResponseDTOList(assignedPapers);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PaperPendingReassignmentResponseDTO> getPapersPendingReassignment(
            String congressCode) {

        // Validar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // Obtener usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // Validar permisos de administración del congreso
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );
        
        // Validar que el usuario tenga su institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // Obtener trabajos pendientes de reasignación
        List<Paper> pendingPapers =
                paperRepository.findPapersPendingReassignment(congressCode);

        // Convertir entidades a DTOs
        return paperMapper.toPaperPendingReassignmentResponseDTOList(pendingPapers);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewerWorkloadResponseDTO> getAvailableReviewersWithWorkload(
            String congressCode) {

        // 1. Validar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // 2. Obtener usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );

        // 4. Validar que el administrador tenga su institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Obtener evaluadores disponibles
        List<CongressParticipant> availableReviewers =
                congressParticipantRepository.findAvailableReviewersByCongressCode(
                        congressCode
                );

        // 6. Obtener la carga de trabajo de los evaluadores
        List<Object[]> workloadResults =
                paperRepository.countPendingPapersByReviewer(congressCode);

        // 7. Convertir los resultados de carga en un Map
        Map<String, Long> workloadByReviewer = new HashMap<>();

        for (Object[] result : workloadResults) {

            String reviewerCode = (String) result[0];
            Long assignedPapersCount = ((Number) result[1]).longValue();

            workloadByReviewer.put(reviewerCode, assignedPapersCount);
        }

        // 8. Construir los DTOs combinando evaluadores y carga de trabajo
        return availableReviewers.stream()
                .map(participant -> {

                    User reviewer = participant.getUser();

                    return ReviewerWorkloadResponseDTO.builder()
                            .reviewerCode(reviewer.getCode())
                            .reviewerFullName(
                                    reviewer.getFirstName() + " " + reviewer.getLastName()
                            )
                            .institution(participant.getInstitution())
                            .assignedPapersCount(
                                    workloadByReviewer.getOrDefault(
                                            reviewer.getCode(),
                                            0L
                                    )
                            )
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaperDetailResponseDTO getPaperById(Long paperId) {
        Paper paper = getPaperByPaperId(paperId);
        validateCurrentUserInstitution(paper.getCongress().getCode());
        validatePaperDetailAccess(paper);

        PaperDetailResponseDTO response = paperMapper.toPaperDetailResponseDTO(paper);

        return applyPaperIdentityVisibility(paper, response);
    }

    @Override
    @Transactional(readOnly = true)
    public Paper getPaperByPaperId(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo no encontrado con ID: " + paperId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaperDetailResponseDTO getPaperByCode(String code) {
        Paper paper = getPaperEntityByCode(code);
        validateCurrentUserInstitution(paper.getCongress().getCode());
        validatePaperDetailAccess(paper);

        PaperDetailResponseDTO response = paperMapper.toPaperDetailResponseDTO(paper);

        return applyPaperIdentityVisibility(paper, response);
    }

    @Override
    @Transactional(readOnly = true)
    public Paper getPaperEntityByCode(String code) {
        return paperRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo no encontrado con código: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperSummaryResponseDTO> getPapersByCongressCode(String congressCode) {
        Congress congress = congressService.getCongressEntityByCode(congressCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congressCode);
        
        List<Paper> papers = paperRepository.findByCongressIdWithDetails(congress.getCongressId());
        return paperMapper.toPaperSummaryResponseDTOList(papers);
    }

    ///----------------------------------------------------------CREATE & UPDATE----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO createPaper(PaperRequestDTO dto) {
        Congress congress = congressService.getCongressEntityByCode(dto.getCongressCode());

        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congress.getCode());
        validateCongressEnabled(congress);
        validateThematicArea(congress, dto.getThematicArea());

        CongressPaperType paperType = validateAndGetPaperType(
                dto.getCongressPaperTypeId(),
                congress
        );

        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        if (dto.getAuthorUserCodes() == null || dto.getAuthorUserCodes().isEmpty()) {
            throw new ArgumentNotValidException(
                    "El trabajo debe tener al menos un autor."
            );
        }

        if (!authenticatedUser.getCode().equals(dto.getAuthorUserCodes().get(0))) {
            throw new AccessDeniedException(
                    "El usuario que crea el trabajo debe ser el autor principal."
            );
        }

        CongressParticipant participant = congressParticipantRepository
                .findActiveParticipantByUserCodeAndCongressCode(
                        authenticatedUser.getCode(),
                        congress.getCode()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no está inscripto activamente en el congreso."
                ));

        RoleName creatorRole = participant.getRole().getName();

        if (creatorRole != RoleName.LISTENER && creatorRole != RoleName.EXPOSITOR) {
            throw new AccessDeniedException(
                    "Solo los participantes con rol LISTENER o EXPOSITOR pueden crear trabajos."
            );
        }

        Paper paper = paperMapper.toEntity(dto);
        paper.setCode(generatePaperCode());
        paper.setVersion("1.0");
        paper.setStatus(PaperStatus.NOT_SUBMITTED);
        paper.setCongress(congress);
        paper.setCongressPaperType(paperType);
        paper.setPay(false);
        paper.setAuthors(new ArrayList<>());
        paper.setKeywords(new HashSet<>());

        // Creador = Main Author (Lógica contenida sólo para el nacimiento del Paper)
        PaperAuthor mainAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(participant.getUser())
                .authorOrder(1L)
                .isMainAuthor(true)
                .isPresentator(true)
                .status(PaperAuthorStatus.ACCEPTED)
                .build();

        paper.getAuthors().add(mainAuthor);

        for (String keyword : dto.getKeywords()) {
            addKeywordInternal(paper, keyword);
        }

        Paper savedPaper = paperRepository.save(paper);

        congressParticipantService.promoteListenerToExpositor(
                congress.getCode(),
                authenticatedUser.getCode()
        );

        PaperDetailResponseDTO response = paperMapper.toPaperDetailResponseDTO(savedPaper);

        return applyPaperIdentityVisibility(savedPaper, response);
    }

    @Override
    @Transactional
    public PaperDetailResponseDTO updatePaper(String paperCode, PaperUpdateRequestDTO dto) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());
        
        // Validar si es autor responsable del trabajo
        validatePaperResponsibleAuthor(paper);
        
        // Solo editable si no ha sido enviado o si necesita revisión
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        Congress congress = paper.getCongress();
        validateThematicArea(congress, dto.getThematicArea());

        CongressPaperType paperType = validateAndGetPaperType(dto.getCongressPaperTypeId(), congress);

        // MapStruct actualiza la entidad existente ignorando colecciones y campos bloqueados
        paperMapper.updateEntityFromDto(dto, paper);
        paper.setCongressPaperType(paperType);

        Paper savedPaper = paperRepository.save(paper);
        PaperDetailResponseDTO response = paperMapper.toPaperDetailResponseDTO(savedPaper);

        return applyPaperIdentityVisibility(savedPaper, response);
    }

    ///----------------------------------------------------------EVALUATION----------------------------------------------------------///

    @Override
    @Transactional
    public MessageResponseDTO assignReviewerToPaper(String paperCode, String reviewerCode) {

        // 1. Obtener el Paper
        Paper paper = getPaperEntityByCode(paperCode);
        String congressCode = paper.getCongress().getCode();

        // 2. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );

        // 4. Validar que el administrador tenga su institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Validar el estado del Paper
        validatePaperEditableState(
                paper,
                PaperStatus.UNDER_EVALUATION,
                PaperStatus.NEEDS_REVISION
        );

        // 6. Verificar que el Paper todavía no tenga un evaluador asignado
        if (paper.getUserReviewer() != null) {
            throw new ResourceAlreadyExistsException(
                    "El trabajo ya tiene un evaluador asignado."
            );
        }

        // 7. Buscar la participación activa del evaluador seleccionado
        CongressParticipant reviewerParticipant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        reviewerCode,
                        congressCode
                ).orElseThrow(() -> new ArgumentNotValidException(
                        "El evaluador no tiene una participación activa en este congreso."
                ));

        User reviewer = reviewerParticipant.getUser();

        // 8. Verificar que la cuenta global esté habilitada
        if (!reviewer.isEnabled()) {
            throw new ArgumentNotValidException(
                    "No se puede asignar un evaluador cuya cuenta esté deshabilitada."
            );
        }

        // 9. Verificar que tenga el rol EVALUATOR en el congreso
        if (reviewerParticipant.getRole().getName() != RoleName.EVALUATOR) {
            throw new ArgumentNotValidException(
                    "El usuario seleccionado no tiene el rol EVALUATOR en este congreso."
            );
        }
        
        // 10. Verificar que el evaluador no sea autor del trabajo
        if (securityEvaluator.isParticipantOfPaper(
                paper.getPaperId(),
                reviewer.getUserId())) {

            throw new ArgumentNotValidException(
                    "Un autor del trabajo no puede ser asignado como evaluador."
            );
        }

        // 11. Registrar la fecha de asignación
        LocalDateTime assignmentDate = DateUtils.now();

        // 12. Asignar el evaluador
        paper.setUserReviewer(reviewer);

        // 13. Guardar los cambios
        Paper savedPaper = paperRepository.save(paper);

        notifyReviewerAssignment(reviewer, savedPaper, assignmentDate);
        
        // 15. Notificar a los autores aceptados sobre la asignación del evaluador (PRE-M02)
        notifyAcceptedAuthorsAboutReviewerAssignment(savedPaper, reviewer);

        return new MessageResponseDTO("Evaluador asignado con éxito");
    }
    
    @Override
    @Transactional
    public MessageResponseDTO reassignReviewerToPaper(String paperCode, String newReviewerCode) {

        // 1. Obtener el Paper
        Paper paper = getPaperEntityByCode(paperCode);
        String congressCode = paper.getCongress().getCode();

        // 2. Obtener usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );

        // 4. Validar que el administrador tenga su institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Validar el estado del Paper
        validatePaperEditableState(
                paper,
                PaperStatus.UNDER_EVALUATION,
                PaperStatus.NEEDS_REVISION
        );

        // 6. Verificar que el Paper tenga un evaluador asignado
        User previousReviewer = paper.getUserReviewer();

        if (previousReviewer == null) {
            throw new ArgumentNotValidException(
                    "El trabajo no tiene un evaluador asignado. No requiere reasignación."
            );
        }

        // 7. Verificar que no exista una evaluación pendiente de liberar
        boolean hasPendingEvaluation =
                evaluationRepository.existsByPaper_CodeAndEvaluatedVersionAndIsReleasedFalse(
                        paperCode,
                        paper.getVersion()
                );

        if (hasPendingEvaluation) {
            throw new ArgumentNotValidException(
                    "No se puede reasignar el trabajo porque tiene una evaluación pendiente de liberación."
            );
        }

        // 8. Verificar que el evaluador anterior haya dejado de ser elegible
        boolean previousReviewerStillEligible =
                previousReviewer.isEnabled()
                && securityEvaluator.hasRoleInCongress(
                        previousReviewer,
                        congressCode,
                        RoleName.EVALUATOR
                );

        if (previousReviewerStillEligible) {
            throw new ArgumentNotValidException(
                    "El evaluador asignado todavía se encuentra habilitado para evaluar en este congreso."
            );
        }

        // 9. Evitar reasignar al mismo evaluador
        if (previousReviewer.getCode().equals(newReviewerCode)) {
            throw new ArgumentNotValidException(
                    "El nuevo evaluador debe ser diferente del evaluador anterior."
            );
        }

        // 10. Buscar la participación activa del nuevo evaluador
        CongressParticipant newReviewerParticipant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                		newReviewerCode,
                        congressCode
                ).orElseThrow(() -> new ArgumentNotValidException(
                        "El nuevo evaluador no tiene una participación activa en este congreso."
                ));

        User newReviewer = newReviewerParticipant.getUser();

        // 11. Verificar que la cuenta global esté habilitada
        if (!newReviewer.isEnabled()) {
            throw new ArgumentNotValidException(
                    "No se puede asignar un evaluador cuya cuenta esté deshabilitada."
            );
        }

        // 12. Verificar que tenga el rol EVALUATOR en este congreso
        if (newReviewerParticipant.getRole().getName() != RoleName.EVALUATOR) {
            throw new ArgumentNotValidException(
                    "El usuario seleccionado no tiene el rol EVALUATOR en este congreso."
            );
        }

        // 13. Verificar que el nuevo evaluador no sea autor del trabajo
        if (securityEvaluator.isParticipantOfPaper(
                paper.getPaperId(),
                newReviewer.getUserId())) {

            throw new ArgumentNotValidException(
                    "Un autor del trabajo no puede ser asignado como evaluador."
            );
        }
        
        // 14. Registrar el momento de la reasignación
        LocalDateTime reassignmentDate = DateUtils.now();

        // 15. Actualizar el evaluador asignado
        paper.setUserReviewer(newReviewer);

        // 16. Guardar los cambios
        Paper savedPaper = paperRepository.save(paper);

        notifyReviewerReassignment(previousReviewer, savedPaper, reassignmentDate);
        notifyReviewerAssignment(newReviewer, savedPaper, reassignmentDate);
        
     	// 19. Notificar a los autores aceptados sobre la nueva asignación del evaluador (PRE-M02)
        notifyAcceptedAuthorsAboutReviewerAssignment(savedPaper, newReviewer);

        return new MessageResponseDTO("Evaluador reasignado con éxito");
    }

    ///----------------------------------------------------------KEYWORDS----------------------------------------------------------///

    @Override
    @Transactional
    public Set<String> addKeywordToPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());

        // Las palabras clave forman parte del contenido del trabajo. No basta
        // con participar en el congreso: solo puede modificarlas su autor
        // responsable.
        validatePaperResponsibleAuthor(paper);
        
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        addKeywordInternal(paper, keyword);
        return paperRepository.save(paper).getKeywords();
    }

    @Override
    @Transactional
    public Set<String> removeKeywordFromPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());

        // Las palabras clave forman parte del contenido del trabajo. No basta
        // con participar en el congreso: solo puede modificarlas su autor
        // responsable.
        validatePaperResponsibleAuthor(paper);
        
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        removeKeywordInternal(paper, keyword);
        return paperRepository.save(paper).getKeywords();
    }

    ///----------------------------------------------------------SUBMIT----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO submitPaper(
            String paperCode,
            boolean confirmDiscardPendingRequests
    ) {
        Paper paper = getPaperEntityByCode(paperCode);

        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());

        // Validar si es autor responsable del trabajo
        validatePaperResponsibleAuthor(paper);

        validatePaperEditableState(
                paper,
                PaperStatus.NOT_SUBMITTED,
                PaperStatus.NEEDS_REVISION
        );

        // Verificar que exista al menos un autor aceptado
        long acceptedAuthorCount = paperAuthorRepository
                .countByPaper_CodeAndStatus(
                        paperCode,
                        PaperAuthorStatus.ACCEPTED
                );

        if (acceptedAuthorCount == 0) {
            throw new ArgumentNotValidException(
                    "El Paper debe tener al menos un autor aceptado antes de enviarlo a revisión."
            );
        }
        
        // Verificar que exista un autor principal aceptado
        List<PaperAuthor> mainAuthors = paper.getAuthors() == null
                ? List.of()
                : paper.getAuthors().stream()
                        .filter(PaperAuthor::isMainAuthor)
                        .toList();

        if (mainAuthors.size() != 1
                || mainAuthors.get(0).getStatus() != PaperAuthorStatus.ACCEPTED) {
            throw new ArgumentNotValidException(
                    "El trabajo debe tener exactamente un autor principal en estado ACCEPTED."
            );
        }

        validateKeywordCountMin(paper.getCongress(), paper.getKeywords().size());

        // Recuperar solicitudes pendientes
        List<PaperAuthor> pendingRequests = paperAuthorRepository
                .findByPaper_CodeAndStatus(
                        paperCode,
                        PaperAuthorStatus.PENDING
                );

        // Exigir confirmación explícita si existen solicitudes pendientes
        if (!pendingRequests.isEmpty() && !confirmDiscardPendingRequests) {
            int pendingCount = pendingRequests.size();

            String message = pendingCount == 1
                    ? "Existe 1 solicitud pendiente de incorporación. "
                    : "Existen " + pendingCount + " solicitudes pendientes de incorporación. ";

            throw new ResourceAlreadyExistsException(
                    message
                    + "Para enviar el trabajo, debe confirmar el descarte de todas las solicitudes pendientes."
            );
        }

        // Descartar solicitudes únicamente si el responsable confirmó
        if (!pendingRequests.isEmpty()) {
            paperAuthorRepository.deleteAll(pendingRequests);
        }
        
        // Determinar el estado anterior del trabajo
        PaperStatus previousStatus = paper.getStatus();

        boolean isInitialSubmission =
                previousStatus == PaperStatus.NOT_SUBMITTED;

        boolean isCorrectionSubmission =
                previousStatus == PaperStatus.NEEDS_REVISION;

        // Capturar el evaluador asignado antes de cambiar el estado
        User assignedReviewer = paper.getUserReviewer();
	
		// Capturar y validar el momento real del envío una única vez.
		LocalDateTime submissionDate = DateUtils.now();
		validateSubmissionDate(paper, previousStatus, submissionDate);
		
		// Cambiar el estado y persistir
		paper.setStatus(PaperStatus.UNDER_EVALUATION);
		paper.setSubmittedAt(submissionDate);
		
		Paper savedPaper = paperRepository.save(paper);
		
		// Construir la respuesta
		PaperDetailResponseDTO response =
		        paperMapper.toPaperDetailResponseDTO(savedPaper);
		
		PaperDetailResponseDTO visibleResponse =
		        applyPaperIdentityVisibility(savedPaper, response);
		

		if (isInitialSubmission) {
		    notifyInitialSubmission(savedPaper, submissionDate);
		}
		
		if (isCorrectionSubmission) {
		    notifyCorrectionSubmission(savedPaper, assignedReviewer, submissionDate);
		}

        notifyDiscardedAuthorJoinRequests(pendingRequests, savedPaper);
		
		return visibleResponse;
    }

    ///----------------------------------------------------------LÓGICA INTERNA----------------------------------------------------------///

    private void notifyReviewerAssignment(User reviewer, Paper paper, LocalDateTime assignmentDate) {
        try {
            emailService.sendPaperAssignedToReviewerNotification(reviewer, paper, assignmentDate);
        } catch (RuntimeException exception) {
            log.error("No se pudo preparar la notificación de asignación para el evaluador {} del trabajo {}.",
                    reviewer.getCode(), paper.getCode(), exception);
        }
    }

    private void notifyReviewerReassignment(User previousReviewer, Paper paper, LocalDateTime reassignmentDate) {
        try {
            emailService.sendPaperReviewerChangedNotification(previousReviewer, paper, reassignmentDate);
        } catch (RuntimeException exception) {
            log.error("No se pudo preparar la notificación de reasignación para el evaluador {} del trabajo {}.",
                    previousReviewer.getCode(), paper.getCode(), exception);
        }
    }

    private void notifyAcceptedAuthorsAboutReviewerAssignment(Paper paper, User reviewer) {
        try {
            List<PaperAuthor> acceptedAuthors = paperAuthorRepository.findByPaper_CodeAndStatus(
                    paper.getCode(), PaperAuthorStatus.ACCEPTED
            );

            for (PaperAuthor paperAuthor : acceptedAuthors) {
                User recipient = paperAuthor.getAuthor();
                try {
                    emailService.sendPaperReviewerAssignedToAuthorNotification(recipient, paper, reviewer);
                } catch (RuntimeException exception) {
                    log.error("No se pudo preparar la notificación de evaluador asignado para el autor {} del trabajo {}.",
                            recipient.getCode(), paper.getCode(), exception);
                }
            }
        } catch (RuntimeException exception) {
            log.error("No se pudieron obtener los autores aceptados para notificar la asignación del evaluador al trabajo {}.",
                    paper.getCode(), exception);
        }
    }

    private void notifyInitialSubmission(Paper paper, LocalDateTime submissionDate) {
        notifyAdministratorsAboutNewSubmission(paper, submissionDate);
        notifyAcceptedAuthorsAboutInitialSubmission(paper, submissionDate);
    }

    private void notifyAdministratorsAboutNewSubmission(Paper paper, LocalDateTime submissionDate) {
        try {
            List<CongressParticipant> administrators = congressParticipantRepository
                    .findActiveEnabledParticipantsByCongressCodeAndRole(
                            paper.getCongress().getCode(), RoleName.ADMINISTRATOR
                    );

            for (CongressParticipant administrator : administrators) {
                try {
                    emailService.sendNewPaperPendingAssignmentNotification(
                            administrator.getUser(), paper, submissionDate
                    );
                } catch (RuntimeException exception) {
                    log.error("No se pudo preparar la notificación de nuevo trabajo para el administrador {} del trabajo {}.",
                            administrator.getUser().getCode(), paper.getCode(), exception);
                }
            }
        } catch (RuntimeException exception) {
            log.error("No se pudieron obtener los administradores para notificar el nuevo trabajo {}.",
                    paper.getCode(), exception);
        }
    }

    private void notifyAcceptedAuthorsAboutInitialSubmission(Paper paper, LocalDateTime submissionDate) {
        notifyAcceptedAuthors(paper, "envío inicial", recipient ->
                emailService.sendPaperSubmissionConfirmationNotification(recipient, paper, submissionDate)
        );
    }

    private void notifyCorrectionSubmission(Paper paper, User assignedReviewer, LocalDateTime submissionDate) {
        notifyReviewerAboutCorrectedVersion(paper, assignedReviewer);
        notifyAcceptedAuthors(paper, "envío de la corrección", recipient ->
                emailService.sendPaperCorrectionSubmissionNotification(recipient, paper, submissionDate)
        );
    }

    private void notifyReviewerAboutCorrectedVersion(Paper paper, User reviewer) {
        if (reviewer == null) {
            return;
        }

        try {
            boolean isReviewerEligible = reviewer.isEnabled()
                    && congressParticipantRepository
                            .findActiveParticipantByUserCodeAndCongressCode(
                                    reviewer.getCode(), paper.getCongress().getCode()
                            )
                            .map(participant -> participant.getRole().getName() == RoleName.EVALUATOR)
                            .orElse(false);

            if (isReviewerEligible) {
                emailService.sendCorrectedPaperVersionToReviewerNotification(reviewer, paper);
            } else {
                log.info("Se omitió la notificación de versión corregida para el trabajo {} porque el evaluador {} ya no es elegible.",
                        paper.getCode(), reviewer.getCode());
            }
        } catch (RuntimeException exception) {
            log.error("No se pudo preparar la notificación de versión corregida para el evaluador {} del trabajo {}.",
                    reviewer.getCode(), paper.getCode(), exception);
        }
    }

    private void notifyAcceptedAuthors(Paper paper, String notificationType, java.util.function.Consumer<User> notification) {
        try {
            List<PaperAuthor> acceptedAuthors = paperAuthorRepository.findByPaper_CodeAndStatus(
                    paper.getCode(), PaperAuthorStatus.ACCEPTED
            );

            for (PaperAuthor paperAuthor : acceptedAuthors) {
                User recipient = paperAuthor.getAuthor();
                try {
                    notification.accept(recipient);
                } catch (RuntimeException exception) {
                    log.error("No se pudo preparar la notificación de {} para el autor {} del trabajo {}.",
                            notificationType, recipient.getCode(), paper.getCode(), exception);
                }
            }
        } catch (RuntimeException exception) {
            log.error("No se pudieron obtener los autores aceptados para notificar el {} del trabajo {}.",
                    notificationType, paper.getCode(), exception);
        }
    }

    private void notifyDiscardedAuthorJoinRequests(List<PaperAuthor> pendingRequests, Paper paper) {
        for (PaperAuthor pendingRequest : pendingRequests) {
            try {
                emailService.sendDiscardedAuthorJoinRequestNotification(pendingRequest.getAuthor(), paper);
            } catch (RuntimeException exception) {
                log.error("No se pudo preparar la notificación de descarte de coautoría para el usuario {} del trabajo {}.",
                        pendingRequest.getAuthor().getCode(), paper.getCode(), exception);
            }
        }
    }

    private CongressPaperType validateAndGetPaperType(Long paperTypeId, Congress congress) {
        CongressPaperType paperType = congressPaperTypeRepository.findById(paperTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de trabajo con ID: " + paperTypeId));

        if (!paperType.getCongress().getCongressId().equals(congress.getCongressId())) {
            throw new ArgumentNotValidException("El tipo de trabajo seleccionado no pertenece al congreso con código " + congress.getCode());
        }
        return paperType;
    }

    private void addKeywordInternal(Paper paper, String keyword) {
        Congress congress = paper.getCongress();
        validateKeywordNotDuplicated(paper.getKeywords(), keyword);
        validateKeywordCountMax(congress, paper.getKeywords().size() + 1);
        validateKeywordRepetitionInTitle(congress, paper.getTitle(), keyword);
        paper.getKeywords().add(keyword);
    }

    private void removeKeywordInternal(Paper paper, String keyword) {
        String actualKeyword = paper.getKeywords().stream()
                .filter(k -> k.equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("La palabra clave '" + keyword + "' no existe."));

        if (paper.getKeywords().size() <= 1) {
            throw new ArgumentNotValidException("No se puede eliminar la última palabra clave.");
        }
        paper.getKeywords().remove(actualKeyword);
    }

    private void validatePaperEditableState(Paper paper, PaperStatus... allowedStates) {
        boolean isAllowed = Arrays.stream(allowedStates).anyMatch(s -> s == paper.getStatus());
        if (!isAllowed) {
            throw new ArgumentNotValidException("No se puede realizar esta acción con el Paper en estado " + paper.getStatus());
        }
    }

    private String generatePaperCode() {
        String code;
        do {
            code = generateRandomNumericCode(CODE_LENGTH);
        } while (paperRepository.existsByCode(code));
        return code;
    }

    private String generateRandomNumericCode(int length) {
        int max = (int) Math.pow(10, length) - 1;
        int randomNumber = new Random().nextInt(max + 1);
        return String.format("%0" + length + "d", randomNumber);
    }

    private void validateCongressEnabled(Congress congress) {
        if (!congress.isEnabled()) {
            throw new ArgumentNotValidException("El congreso está deshabilitado.");
        }
    }

    private void validateThematicArea(Congress congress, String thematicArea) {
        boolean isValid = congress.getThematicAreas().stream()
                .anyMatch(area -> area.equalsIgnoreCase(thematicArea));
        if (!isValid) {
            throw new ArgumentNotValidException("El área temática no es válida para este congreso.");
        }
    }

    private void validateSubmissionDate(
            Paper paper,
            PaperStatus previousStatus,
            LocalDateTime submissionDate
    ) {
        Congress congress = paper.getCongress();

        if (previousStatus == PaperStatus.NOT_SUBMITTED) {
            validateInitialSubmissionWithinWindow(congress, submissionDate);
            return;
        }

        if (previousStatus == PaperStatus.NEEDS_REVISION) {
            validateCorrectionSubmissionDeadline(paper, submissionDate);
        }
    }

    private void validateInitialSubmissionWithinWindow(
            Congress congress,
            LocalDateTime submissionDate
    ) {
        if (submissionDate.isBefore(congress.getPresentationStartDate())
                || submissionDate.isAfter(congress.getPresentationEndDate())) {
            throw new ArgumentNotValidException(
                    "El envío inicial debe realizarse dentro del período de presentación de trabajos."
            );
        }
    }

    private void validateCorrectionSubmissionDeadline(Paper paper, LocalDateTime submissionDate) {
        Congress congress = paper.getCongress();

        if (!submissionDate.isBefore(congress.getStartDate())) {
            throw new ArgumentNotValidException(
                    "No se pueden enviar correcciones una vez iniciado el evento."
            );
        }

        Evaluation correctionEvaluation = evaluationRepository
                .findByPaper_CodeOrderByEvaluationDateDesc(paper.getCode())
                .stream()
                .filter(Evaluation::isReleased)
                .filter(evaluation -> evaluation.getNewStatus() == PaperStatus.NEEDS_REVISION)
                .findFirst()
                .orElseThrow(() -> new ArgumentNotValidException(
                        "No se encontró una evaluación publicada que habilite el reenvío de la corrección."
                ));

        LocalDateTime newDeadline = correctionEvaluation.getNewDeadline();
        if (newDeadline == null || submissionDate.isAfter(newDeadline)) {
            throw new ArgumentNotValidException(
                    "El plazo definido para enviar la corrección ha vencido."
            );
        }
    }

    private void validateKeywordNotDuplicated(Set<String> existingKeywords, String newKeyword) {
        if (existingKeywords.stream().anyMatch(k -> k.equalsIgnoreCase(newKeyword))) {
            throw new ArgumentNotValidException("La palabra clave ya fue agregada (no se permiten duplicados).");
        }
    }

    private void validateKeywordRepetitionInTitle(Congress congress, String title, String keyword) {
        if (!congress.isKeywordRepetition() && title != null && title.toLowerCase().contains(keyword.toLowerCase())) {
            throw new ArgumentNotValidException("El congreso no permite que las palabras clave aparezcan repetidas en el título.");
        }
    }

    private void validateKeywordCountMax(Congress congress, int totalAfterOperation) {
        if (congress.getMaxKeywords() != null && totalAfterOperation > congress.getMaxKeywords()) {
            throw new ArgumentNotValidException("Supera el máximo de palabras clave del congreso.");
        }
    }

    private void validateKeywordCountMin(Congress congress, int totalKeywords) {
        if (congress.getMinKeywords() != null && totalKeywords < congress.getMinKeywords()) {
            throw new ArgumentNotValidException("No alcanza el mínimo de palabras clave requeridas.");
        }
    }
    
    private void validateCurrentUserInstitution(String congressCode) {
        institutionValidator.validateAuthenticatedParticipantInstitution(
                congressCode
        );
    }
    
    private void validatePaperDetailAccess(Paper paper) {

        var authenticatedUser = authorizationService.getAuthenticatedUserEntity();
        String congressCode = paper.getCongress().getCode();

        // El superadministrador y el administrador del congreso
        // pueden consultar cualquier trabajo del congreso.
        if (securityEvaluator.isSuperAdmin(authenticatedUser)
                || securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            return;
        }

        // Los evaluadores solamente pueden consultar trabajos asignados.
        if (securityEvaluator.isAssignedReviewer(authenticatedUser, paper)) {
            return;
        }

        // Los autores solamente pueden consultar trabajos
        // en los que tengan una participación aceptada.
        if (securityEvaluator.isAcceptedAuthor(authenticatedUser, paper)) {
            return;
        }

        throw new AccessDeniedException(
                "No tiene permisos para consultar el detalle de este trabajo"
        );
    }
    
    private PaperDetailResponseDTO applyPaperIdentityVisibility(
            Paper paper,
            PaperDetailResponseDTO response
    ) {
    	User authenticatedUser = authorizationService.getAuthenticatedUserEntity();
        String congressCode = paper.getCongress().getCode();

        // Los administradores pueden ver ambas identidades.
        if (securityEvaluator.isSuperAdmin(authenticatedUser)
                || securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            return response;
        }

        // Los evaluadores no pueden conocer la identidad de los autores.
        if (securityEvaluator.isAssignedReviewer(authenticatedUser, paper)) {
            response.setAuthors(null);
            return response;
        }

        // Los autores conocen a los evaluadores únicamente
        // en congresos con modalidad simple ciego.
        if (securityEvaluator.isAcceptedAuthor(authenticatedUser, paper)) {
            if (!paper.getCongress().isSingleBlind()) {
                response.setReviewerCode(null);
                response.setReviewerFullName(null);
            }
        }

        return response;
    }
    
    private void validatePaperResponsibleAuthor(Paper paper) {
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        boolean isResponsibleAuthor = paper.getAuthors() != null
                && paper.getAuthors().stream()
                        .anyMatch(paperAuthor ->
                                paperAuthor.isMainAuthor()
                                        && paperAuthor.getAuthor().getUserId().equals(authenticatedUser.getUserId())
                        );

        if (!isResponsibleAuthor) {
            throw new AccessDeniedException(
                    "Solo el autor responsable puede realizar esta operación."
            );
        }
    }
}
