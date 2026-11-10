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
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final CongressParticipantRepository congressParticipantRepository;
    private final CongressPaperTypeRepository congressPaperTypeRepository;
    private final EvaluationRepository evaluationRepository;
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;
    private final AuthorizationService authorizationService;
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

    	// Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());

        return paperMapper.toPaperDetailResponseDTO(paper);
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

    	// Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());

        return paperMapper.toPaperDetailResponseDTO(paper);
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
        validatePresentationDateWithinWindow(congress, dto.getPresentationDate());

        CongressPaperType paperType = validateAndGetPaperType(dto.getCongressPaperTypeId(), congress);

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
        String creatorCode = dto.getAuthorUserCodes().get(0);
        CongressParticipant participant = congressParticipantRepository
        		.findActiveParticipantByUserCodeAndCongressCode(creatorCode, congress.getCode())
                .orElseThrow(() -> new ArgumentNotValidException("El creador no está inscripto en el congreso."));

        PaperAuthor mainAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(participant.getUser())
                .authorOrder(1L)
                .isMainAuthor(true)
                .isPresentator(false)
                .status(PaperAuthorStatus.ACCEPTED)
                .build();
        
        paper.getAuthors().add(mainAuthor);

        for (String keyword : dto.getKeywords()) {
            addKeywordInternal(paper, keyword);
        }

        return paperMapper.toPaperDetailResponseDTO(paperRepository.save(paper));
    }

    @Override
    @Transactional
    public PaperDetailResponseDTO updatePaper(String paperCode, PaperUpdateRequestDTO dto) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());
        
        // Solo editable si no ha sido enviado o si necesita revisión
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        Congress congress = paper.getCongress();
        validateThematicArea(congress, dto.getThematicArea());
        validatePresentationDateWithinWindow(congress, dto.getPresentationDate());

        CongressPaperType paperType = validateAndGetPaperType(dto.getCongressPaperTypeId(), congress);

        // MapStruct actualiza la entidad existente ignorando colecciones y campos bloqueados
        paperMapper.updateEntityFromDto(dto, paper);
        paper.setCongressPaperType(paperType);

        return paperMapper.toPaperDetailResponseDTO(paperRepository.save(paper));
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

        // 11. Asignar el evaluador
        paper.setUserReviewer(reviewer);

        // 12. Guardar los cambios
        paperRepository.save(paper);

        return new MessageResponseDTO("Evaluador asignado con éxito");
    }
    
    @Override
    @Transactional
    public MessageResponseDTO reassignReviewerToPaper(
            String paperCode,
            String newReviewerCode) {

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
        
        // 14. Actualizar el evaluador asignado
        paper.setUserReviewer(newReviewer);
        paperRepository.save(paper);

        return new MessageResponseDTO("Evaluador reasignado con éxito");
    }

    ///----------------------------------------------------------KEYWORDS----------------------------------------------------------///

    @Override
    @Transactional
    public Set<String> addKeywordToPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());
        
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
        
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        removeKeywordInternal(paper, keyword);
        return paperRepository.save(paper).getKeywords();
    }

    ///----------------------------------------------------------SUBMIT----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO submitPaper(String paperCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper.getCongress().getCode());
        
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            throw new ArgumentNotValidException("El Paper debe tener al menos un autor antes de enviarlo a revisión.");
        }

        validateKeywordCountMin(paper.getCongress(), paper.getKeywords().size());

        paper.setStatus(PaperStatus.UNDER_EVALUATION);
        return paperMapper.toPaperDetailResponseDTO(paperRepository.save(paper));
    }

    ///----------------------------------------------------------LÓGICA INTERNA----------------------------------------------------------///

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

    private void validatePresentationDateWithinWindow(Congress congress, LocalDateTime presentationDate) {
        if (presentationDate.isBefore(congress.getPresentationStartDate())
                || presentationDate.isAfter(congress.getPresentationEndDate())) {
            throw new ArgumentNotValidException("La fecha de presentación debe estar dentro de la ventana del congreso.");
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
}