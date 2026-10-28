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
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.UserService;
import lombok.RequiredArgsConstructor;
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
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;
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

    ///----------------------------------------------------------CREATE & UPDATE----------------------------------------------------------///

    @Override
    @Transactional
    public PaperDetailResponseDTO createPaper(PaperRequestDTO dto) {
        Congress congress = congressService.getCongressByCode(dto.getCongressCode());
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
                .findUserActiveParticipantOfCongress(creatorCode, congress.getCode())
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
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        User paperReviewer = paper.getUserReviewer();
        if (paperReviewer != null) {
            if (!paperReviewer.getCode().equals(reviewerCode)) {
                throw new ResourceAlreadyExistsException("El trabajo ya tiene otro evaluador asignado.");
            } else {
                throw new ResourceAlreadyExistsException("Este evaluador ya está asignado a este trabajo.");
            }
        }

        CongressParticipant reviewerParticipant = congressParticipantRepository
                .findUserActiveParticipantOfCongress(reviewerCode, paper.getCongress().getCode())
                .orElseThrow(() -> new ArgumentNotValidException("El evaluador no pertenece al congreso."));

        if (!securityEvaluator.hasRoleInCongress(reviewerParticipant.getUser(), paper.getCongress().getCode(), RoleName.EVALUATOR)) {
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
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        addKeywordInternal(paper, keyword);
        return paperRepository.save(paper).getKeywords();
    }

    @Override
    @Transactional
    public Set<String> removeKeywordFromPaper(String paperCode, String keyword) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        removeKeywordInternal(paper, keyword);
        return paperRepository.save(paper).getKeywords();
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
}