package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressPaperType;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperMapper;
import com.tfi.gestion_congresos_backend.repository.CongressPaperTypeRepository;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final CongressParticipantRepository congressParticipantRepository; 
    private final RoleRepository roleRepository; 
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;
    private final CongressPaperTypeRepository congressPaperTypeRepository;
    private final SecurityEvaluator securityEvaluator;

    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperSummaryResponseDTO> getAssignedPapers(String reviewerCode) {
        if (!userService.existsByCode(reviewerCode)) {
            throw new ResourceNotFoundException("Evaluador no encontrado con el código: " + reviewerCode);
        }
        List<Paper> assignedPapers = paperRepository.findByUserReviewer_Code(reviewerCode);
        return paperMapper.toPaperSummaryResponseDTOList(assignedPapers);
    }

    @Override
    @Transactional(readOnly = true)
    public PaperDetailResponseDTO getPaperById(Long paperId) {
        return paperMapper.toPaperDetailResponseDTO(getPaperByPaperId(paperId));
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
        return paperMapper.toPaperDetailResponseDTO(getPaperEntityByCode(code));
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
        Congress congress = congressService.getCongressByCode(congressCode);
        List<Paper> papers = paperRepository.findByCongressIdWithDetails(congress.getCongressId());
        return paperMapper.toPaperSummaryResponseDTOList(papers);
    }

    ///----------------------------------------------------------CREATE----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO createPaper(PaperRequestDTO dto) {

        Congress congress = congressService.getCongressByCode(dto.getCongressCode());
        validateCongressEnabled(congress);
        validateThematicArea(congress, dto.getThematicArea());
        validatePresentationDateWithinWindow(congress, dto.getPresentationDate());

        CongressPaperType paperType = congressPaperTypeRepository.findById(dto.getCongressPaperTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de trabajo con ID: " + dto.getCongressPaperTypeId()));

        if (!paperType.getCongress().getCongressId().equals(congress.getCongressId())) {
            throw new ArgumentNotValidException("El tipo de trabajo seleccionado no pertenece al congreso con código " + congress.getCode());
        }

        Paper paper = paperMapper.toEntity(dto);
        paper.setCode(generatePaperCode()); // Server-side code generation
        paper.setVersion("1.0");
        paper.setStatus(PaperStatus.NOT_SUBMITTED);
        paper.setCongress(congress);
        paper.setCongressPaperType(paperType);
        paper.setPay(false); // Siempre nace en false
        paper.setAuthors(new ArrayList<>());
        paper.setKeywords(new HashSet<>());

        // Creador = Main Author
        User creator = userService.getUserByUserCode(dto.getAuthorUserCodes().get(0));
        addAuthorInternal(paper, creator, true, true);

        // Co-autores ingresan por la peticion

        for (String keyword : dto.getKeywords()) {
            addKeywordInternal(paper, keyword);
        }

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toPaperDetailResponseDTO(savedPaper);
    }

    ///----------------------------------------------------------AUTHORS----------------------------------------------------------///

    @Override
    @Transactional
    public List<AuthorResponseDTO> requestToJoinPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        User user = userService.getUserByUserCode(userCode);
        addAuthorInternal(paper, user, false, false);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> acceptAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        PaperAuthor request = findPendingRequest(paper, userCode);
        validateMaxAuthors(paper.getCongress(), countAccepted(paper) + 1); // Valida límite contando solo ACCEPTED

        request.setStatus(PaperAuthorStatus.ACCEPTED);
        request.setAuthorOrder(countAccepted(paper) + 1);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> rejectAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        PaperAuthor request = findPendingRequest(paper, userCode);
        paper.getAuthors().remove(request); // Orphan removal se encarga de borrarlo de la base

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> addAuthorToPaper(String paperCode, String userCode) {
        // NOTA: 
        // Definir validaciones para este metodo
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        CongressParticipant targetParticipant = congressParticipantRepository
                .findUserParticipantOfCongress(userCode, paper.getCongress().getCode())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no está inscripto en este congreso."));

        RoleName currentRole = targetParticipant.getRole().getName();

        if (currentRole == RoleName.EVALUATOR) {
            throw new ArgumentNotValidException("El usuario es EVALUADOR en este congreso. Utilice el flujo de evaluación.");
        }

        if (currentRole == RoleName.LISTENER) {
            Role expositorRole = roleRepository.findByName(RoleName.EXPOSITOR)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol EXPOSITOR no encontrado."));
            targetParticipant.setRole(expositorRole);
            congressParticipantRepository.save(targetParticipant);
            // TODO: Notificar al oyente su ascenso a EXPOSITOR
        }

        addAuthorInternal(paper, targetParticipant.getUser(), false, true);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> removeAuthorFromPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        removeAuthorInternal(paper, userCode);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public MessageResponseDTO assignReviewerToPaper(String paperCode, String reviewerCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        User paperReviewer = paper.getUserReviewer();

        if (paperReviewer != null) {
            if (!paperReviewer.getCode().equals(reviewerCode)) {
                throw new ResourceAlreadyExistsException("El trabajo ya tiene otro evaluador asignado."); // 4092, 
            } else {
                throw new ResourceAlreadyExistsException("Este evaluador ya está asignado a este trabajo."); // 4092, 
            }
        }

        CongressParticipant reviewerParticipant = congressParticipantRepository
                .findUserParticipantOfCongress( reviewerCode, paper.getCongress().getCode())
                .orElseThrow(() -> new ArgumentNotValidException("El evaluador no pertenece al mismo congreso que el trabajo."));

        
        if (securityEvaluator.hasRoleInCongress(paperReviewer, paper.getCongress().getCode(), RoleName.EVALUATOR)) {
            throw new ArgumentNotValidException("El usuario seleccionado no tiene rol EVALUADOR en este congreso.");
        }

        paper.setUserReviewer(reviewerParticipant.getUser());
        paperRepository.save(paper);

        return new MessageResponseDTO("Evaluador asignado con éxito");
    }

    ///----------------------------------------------------------KEYWORDS----------------------------------------------------------///

    @Override
    @Transactional
    public Set<String> addKeywordToPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION); // Editables en NEEDS_REVISION

        addKeywordInternal(paper, keyword);
        Paper savedPaper = paperRepository.save(paper);
        return savedPaper.getKeywords();
    }

    @Override
    @Transactional
    public Set<String> removeKeywordFromPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        removeKeywordInternal(paper, keyword);
        Paper savedPaper = paperRepository.save(paper);
        return savedPaper.getKeywords();
    }

    ///----------------------------------------------------------SUBMIT----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO submitPaper(String paperCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            throw new ArgumentNotValidException("El Paper debe tener al menos un autor antes de enviarlo a revisión.");
        }

        
        validateKeywordCountMin(paper.getCongress(), paper.getKeywords().size()); // Se valida mínimo al enviar, no al crear

        paper.setStatus(PaperStatus.UNDER_EVALUATION);
        Paper saved = paperRepository.save(paper);
        return paperMapper.toPaperDetailResponseDTO(saved);
    }

    ///----------------------------------------------------------LÓGICA INTERNA----------------------------------------------------------///

    private void addAuthorInternal(Paper paper, User user, boolean isMainAuthor, boolean immediateAccept) {
        Congress congress = paper.getCongress();

        // Validar que el usuario sea participante del congreso ANTES de vincularlo
        CongressParticipant participant = congressParticipantRepository
                .findUserParticipantOfCongress( user.getCode(),congress.getCode())
                .orElseThrow(() -> new ArgumentNotValidException("El usuario no está inscripto en el congreso con código " + congress.getCode()));

        boolean alreadyLinked = paper.getAuthors().stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(user.getUserId()));
        
        if (alreadyLinked) {
            throw new ResourceAlreadyExistsException("El usuario ya tiene una relación (pendiente o aceptada) con este Paper."); // 409
        }

        PaperAuthorStatus status;
        PaperAuthor newPaperAuthor;
        long order;
        //Si no es el autor principal, hacemos comprobaciones de orden y estado de la solicitud
        if (!isMainAuthor){

            if (immediateAccept) {
                validateMaxAuthors(congress, countAccepted(paper) + 1);
                status = PaperAuthorStatus.ACCEPTED;
                order = countAccepted(paper) + 1;
            }
            else {
                // Regla de negocio: para unirse estándar (solicitud o creador), debería ser EXPOSITOR
                if (participant.getRole().getName() != RoleName.LISTENER) {
                    throw new ArgumentNotValidException("Solo los participantes con rol LISTENER pueden solicitar unirse a un trabajo.");
                }
                status = PaperAuthorStatus.PENDING;
                order = 0; // Orden 0 mientras está pendiente
            }
             newPaperAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(user)
                .authorOrder(order)
                .isMainAuthor(isMainAuthor)
                .isPresentator(false)
                .status(status)
                .build();

        }else{
            newPaperAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(user)
                .authorOrder((long)1)
                .isMainAuthor(isMainAuthor)
                .isPresentator(false)
                .status(PaperAuthorStatus.ACCEPTED)
                .build();
        }




        paper.getAuthors().add(newPaperAuthor);
    }

    private void removeAuthorInternal(Paper paper, String userCode) {
        PaperAuthor toRemove = paper.getAuthors().stream()
                .filter(pa -> pa.getAuthor().getCode().equals(userCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene relación con este Paper.")); // 404

        if (toRemove.isMainAuthor()) {
            throw new ArgumentNotValidException("No se puede eliminar al autor principal del Paper."); // Nunca eliminar mainAuthor
        }

        if (toRemove.getStatus() == PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("Este usuario tiene una solicitud pendiente. Usá el endpoint de rechazo (reject)."); // No usar remove para PENDING
        }

        paper.getAuthors().remove(toRemove);

        // Reordenamiento compacto sin huecos
        List<PaperAuthor> remainingAccepted = paper.getAuthors().stream()
                .filter(pa -> pa.getStatus() == PaperAuthorStatus.ACCEPTED)
                .sorted(Comparator.comparingLong(PaperAuthor::getAuthorOrder))
                .toList();

        int order = 1;
        for (PaperAuthor pa : remainingAccepted) {
            pa.setAuthorOrder((long) order++);
        }
    }

    private PaperAuthor findPendingRequest(Paper paper, String userCode) {
        return paper.getAuthors().stream()
                .filter(pa -> pa.getAuthor().getCode().equals(userCode) && pa.getStatus() == PaperAuthorStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No existe una solicitud pendiente de este usuario para este Paper."));
    }

    private long countAccepted(Paper paper) {
        return paper.getAuthors().stream().filter(pa -> pa.getStatus() == PaperAuthorStatus.ACCEPTED).count();
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
                .filter(k -> k.equalsIgnoreCase(keyword)) // Case-insensitive comparisson
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("La palabra clave '" + keyword + "' no existe en este Paper."));

        if (paper.getKeywords().size() <= 1) {
            throw new ArgumentNotValidException("No se puede eliminar la última palabra clave."); // Nunca dejar en 0
        }

        paper.getKeywords().remove(actualKeyword);
    }

    private void validatePaperEditableState(Paper paper, PaperStatus... allowedStates) {
        boolean isAllowed = Arrays.stream(allowedStates).anyMatch(s -> s == paper.getStatus());
        if (!isAllowed) {
            throw new ArgumentNotValidException("No se puede realizar esta acción con el Paper en estado " + paper.getStatus());
        }
    }

    private void validateMaxAuthors(Congress congress, long totalAuthorsAfterOperation) {
        Integer max = congress.getMaxNumberOfAuthors();
        if (max != null && totalAuthorsAfterOperation > max) { // Check de null explícito requerido por la skill
            throw new ArgumentNotValidException("El congreso supera el máximo de autores permitidos.");
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
            throw new ArgumentNotValidException("El congreso está deshabilitado."); // Validación habilitada
        }
    }

    private void validateThematicArea(Congress congress, String thematicArea) {
        boolean isValid = congress.getThematicAreas().stream()
                .anyMatch(area -> area.equalsIgnoreCase(thematicArea)); // Case-insensitive validation
        if (!isValid) {
            throw new ArgumentNotValidException("El área temática no es válida para este congreso.");
        }
    }

    private void validatePresentationDateWithinWindow(Congress congress, LocalDateTime presentationDate) {
        if (presentationDate.isBefore(congress.getPresentationStartDate())
                || presentationDate.isAfter(congress.getPresentationEndDate())) { // Validar fecha de ventana
            throw new ArgumentNotValidException("La fecha de presentación debe estar dentro de la ventana del congreso.");
        }
    }

    private void validateKeywordNotDuplicated(Set<String> existingKeywords, String newKeyword) {
        boolean isDuplicate = existingKeywords.stream().anyMatch(k -> k.equalsIgnoreCase(newKeyword)); // Case-insensitive no dupes
        if (isDuplicate) {
            throw new ArgumentNotValidException("La palabra clave ya fue agregada (no se permiten duplicados).");
        }
    }

    private void validateKeywordRepetitionInTitle(Congress congress, String title, String keyword) {
        if (!congress.isKeywordRepetition() && title != null && title.toLowerCase().contains(keyword.toLowerCase())) { // Substring repetición
            throw new ArgumentNotValidException("El congreso no permite que las palabras clave aparezcan repetidas en el título.");
        }
    }

    private void validateKeywordCountMax(Congress congress, int totalAfterOperation) {
        Integer max = congress.getMaxKeywords();
        if (max != null && totalAfterOperation > max) { // Check de null explícito
            throw new ArgumentNotValidException("Supera el máximo de palabras clave del congreso.");
        }
    }

    private void validateKeywordCountMin(Congress congress, int totalKeywords) {
        Integer min = congress.getMinKeywords();
        if (min != null && totalKeywords < min) { // Check de null explícito
            throw new ArgumentNotValidException("No alcanza el mínimo de palabras clave requeridas.");
        }
    }
}