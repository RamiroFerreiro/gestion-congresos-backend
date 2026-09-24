package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressPaperType;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperMapper;
import com.tfi.gestion_congresos_backend.repository.CongressPaperTypeRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
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
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;
    private final CongressPaperTypeRepository congressPaperTypeRepository;

    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponseDTO> getAssignedPapers(String reviewerCode) {
        if (!userService.existsByCode(reviewerCode)) {
            throw new ResourceNotFoundException("Evaluador no encontrado con el código: " + reviewerCode);
        }
        List<Paper> assignedPapers = paperRepository.findByUserReviewer_Code(reviewerCode);
        return paperMapper.toPaperResponseDTOList(assignedPapers);
    }

    // Se mantiene tal cual: uso interno / administrativo por ID
    @Override
    @Transactional(readOnly = true)
    public PaperResponseDTO getPaperById(Long paperId) {
        return paperMapper.toPaperResponseDTO(getPaperByPaperId(paperId));
    }

    @Override
    @Transactional(readOnly = true)
    public Paper getPaperByPaperId(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo no encontrado con ID: " + paperId));
    }

    // 👇 nuevo: el que ahora usa toda la lógica de negocio
    @Override
    @Transactional(readOnly = true)
    public PaperResponseDTO getPaperByCode(String code) {
        return paperMapper.toPaperResponseDTO(getPaperEntityByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public Paper getPaperEntityByCode(String code) {
        return paperRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo no encontrado con código: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponseDTO> getPapersByCongressCode(String congressCode) {
        if (!congressService.existsByCode(congressCode)) {
            throw new ResourceNotFoundException("Congreso no encontrado con código: " + congressCode);
        }
        Congress congress = congressService.getCongressByCode(congressCode);
        List<Paper> papers = paperRepository.findByCongressIdWithDetails(congress.getCongressId());
        return papers.stream().map(paperMapper::toPaperResponseDTO).toList();
    }

    ///----------------------------------------------------------CREATE----------------------------------------------------------///

    @Override
    @Transactional
    public PaperResponseDTO createPaper(PaperRequestDTO dto) {

        Congress congress = congressService.getCongressByCode(dto.getCongressCode());


        validateCongressEnabled(congress);
        validateThematicArea(congress, dto.getThematicArea());
        validatePresentationDateWithinWindow(congress, dto.getPresentationDate());

        CongressPaperType paperType = congressPaperTypeRepository.findById(dto.getCongressPaperTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No se encontró el tipo de trabajo con ID: " + dto.getCongressPaperTypeId()));

        if (!paperType.getCongress().getCongressId().equals(congress.getCongressId())) {
            throw new ArgumentNotValidException(
                "El tipo de trabajo seleccionado no pertenece al congreso con código " + congress.getCode());
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

        User creator = userService.getUserByUserId(dto.getAuthorUserIds().get(0));
        addAuthorInternal(paper, creator, true, true);

        for (int i = 1; i < dto.getAuthorUserIds().size(); i++) {
            User user = userService.getUserByUserId(dto.getAuthorUserIds().get(i));
            addAuthorInternal(paper, user, false, false);
        }

        for (String keyword : dto.getKeywords()) {
            addKeywordInternal(paper, keyword);
        }

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toPaperResponseDTO(savedPaper);
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
        validateMaxAuthors(paper.getCongress(), countAccepted(paper) + 1);

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
        paper.getAuthors().remove(request);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> addAuthorToPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        User user = userService.getUserByUserCode(userCode);
        if (user.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new ArgumentNotValidException(
                "Este endpoint es exclusivo para agregar administradores sin pasar por el flujo de solicitud. " +
                "Para agregar autores EXPOSITOR, usá el flujo de solicitud (requestToJoinPaper).");
        }

        addAuthorInternal(paper, user, false, true);

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

        User paperReviewer = paper.getUserReviewer();
        if (paperReviewer != null) {
            if (!paperReviewer.getCode().equals(reviewerCode)) {
                throw new ResourceAlreadyExistsException(
                    "El trabajo con código " + paperCode + " ya tiene asignado un evaluador (" +
                    paperReviewer.getFirstName() + " " + paperReviewer.getLastName() + ")."
                );
            } else {
                throw new ResourceAlreadyExistsException(
                    "El evaluador con código " + reviewerCode + " ya está asignado a este trabajo."
                );
            }
        }

        User reviewer = userService.getUserByUserCode(reviewerCode);
        Congress congress = paper.getCongress();

        boolean isParticipantInCongress = congressService.existsByCongressCodeAndUserCodeAndRoleName(congress.getCode(), reviewerCode, RoleName.EVALUATOR);
        if (!isParticipantInCongress) {
            throw new ArgumentNotValidException(
                "El evaluador con código " + reviewerCode + " no pertenece al mismo congreso que el trabajo con código " + paperCode + ".");
        }

        paper.setUserReviewer(reviewer);
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
    public PaperResponseDTO submitPaper(String paperCode) {
        Paper paper = getPaperEntityByCode(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            throw new ArgumentNotValidException("El Paper debe tener al menos un autor antes de enviarlo a revisión");
        }

        validateKeywordCountMin(paper.getCongress(), paper.getKeywords().size());

        paper.setStatus(PaperStatus.UNDER_EVALUATION);
        Paper saved = paperRepository.save(paper);
        return paperMapper.toPaperResponseDTO(saved);
    }

    ///----------------------------------------------------------LÓGICA INTERNA Y HELPERS----------------------------------------------------------///
    // (idénticos a la versión anterior — no dependen de ID vs code, operan sobre el objeto Paper ya resuelto)

    private void addAuthorInternal(Paper paper, User user, boolean isMainAuthor, boolean immediateAccept) {
        Congress congress = paper.getCongress();

        boolean isAuthorInCongress = congressService.existsByCongressCodeAndUserCodeAndRoleName(congress.getCode(), user.getCode(), RoleName.EXPOSITOR);
        if (!isAuthorInCongress && user.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new ArgumentNotValidException(
                "El usuario con ID " + user.getUserId() + " no está inscripto como EXPOSITOR en el congreso con código " + congress.getCode());
        }

        boolean alreadyLinked = paper.getAuthors().stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(user.getUserId()));
        if (alreadyLinked) {
            throw new ResourceAlreadyExistsException(
                "El usuario con ID " + user.getUserId() + " ya tiene una relación (pendiente o aceptada) con este Paper");
        }

        PaperAuthorStatus status;
        long order;

        if (immediateAccept) {
            validateMaxAuthors(congress, countAccepted(paper) + 1);
            status = PaperAuthorStatus.ACCEPTED;
            order = countAccepted(paper) + 1;
        } else {
            status = PaperAuthorStatus.PENDING;
            order = 0;
        }

        PaperAuthor newPaperAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(user)
                .authorOrder(order)
                .isMainAuthor(isMainAuthor)
                .isPresentator(false)
                .status(status)
                .build();

        paper.getAuthors().add(newPaperAuthor);
    }

    private void removeAuthorInternal(Paper paper, String userCode) {
        PaperAuthor toRemove = paper.getAuthors().stream()
                .filter(pa -> pa.getAuthor().getCode().equals(userCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "El usuario con código " + userCode + " no tiene relación con este Paper"));

        if (toRemove.isMainAuthor()) {
            throw new ArgumentNotValidException(
                "No se puede eliminar al autor principal del Paper. El Paper debe tener siempre un autor principal.");
        }

        if (toRemove.getStatus() == PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException(
                "Este usuario tiene una solicitud pendiente, no un rol aceptado. Usá el endpoint de rechazo (reject) en vez de remove.");
        }

        paper.getAuthors().remove(toRemove);

        List<PaperAuthor> remainingAccepted = paper.getAuthors().stream()
                .filter(pa -> pa.getStatus() == PaperAuthorStatus.ACCEPTED)
                .sorted(Comparator.comparingLong(PaperAuthor::getAuthorOrder))
                .toList();

        int order = 1;
        for (PaperAuthor pa : remainingAccepted) {
            pa.setAuthorOrder((long)order++);
        }
    }

    private PaperAuthor findPendingRequest(Paper paper, String userCode) {
        return paper.getAuthors().stream()
                .filter(pa -> pa.getAuthor().getCode().equals(userCode) && pa.getStatus() == PaperAuthorStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No existe una solicitud pendiente del usuario con código " + userCode + " para este Paper"));
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
                .filter(k -> k.equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "La palabra clave '" + keyword + "' no existe en este Paper"));

        if (paper.getKeywords().size() <= 1) {
            throw new ArgumentNotValidException(
                "No se puede eliminar la última palabra clave. El Paper debe tener al menos una.");
        }

        paper.getKeywords().remove(actualKeyword);
    }

    private void validatePaperEditableState(Paper paper, PaperStatus... allowedStates) {
        boolean isAllowed = Arrays.stream(allowedStates).anyMatch(s -> s == paper.getStatus());
        if (!isAllowed) {
            throw new ArgumentNotValidException(
                "No se puede realizar esta acción con el Paper en estado " + paper.getStatus() +
                ". Estados permitidos: " + Arrays.toString(allowedStates));
        }
    }

    private void validateMaxAuthors(Congress congress, long totalAuthorsAfterOperation) {
        Integer max = congress.getMaxNumberOfAuthors();
        if (max != null && totalAuthorsAfterOperation > max) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' permite un máximo de " + max +
                " autores por trabajo. Cantidad solicitada: " + totalAuthorsAfterOperation + ".");
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
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' está deshabilitado. No se pueden cargar trabajos.");
        }
    }

    private void validateThematicArea(Congress congress, String thematicArea) {
        boolean isValid = congress.getThematicAreas().stream()
                .anyMatch(area -> area.equalsIgnoreCase(thematicArea));
        if (!isValid) {
            throw new ArgumentNotValidException(
                "El área temática '" + thematicArea + "' no es válida para el congreso '" + congress.getName() + "'.");
        }
    }

    private void validatePresentationDateWithinWindow(Congress congress, LocalDateTime presentationDate) {
        if (presentationDate.isBefore(congress.getPresentationStartDate())
                || presentationDate.isAfter(congress.getPresentationEndDate())) {
            throw new ArgumentNotValidException(
                "La fecha de presentación debe estar entre " + congress.getPresentationStartDate() +
                " y " + congress.getPresentationEndDate() + " para el congreso '" + congress.getName() + "'.");
        }
    }

    private void validateKeywordNotDuplicated(Set<String> existingKeywords, String newKeyword) {
        boolean isDuplicate = existingKeywords.stream().anyMatch(k -> k.equalsIgnoreCase(newKeyword));
        if (isDuplicate) {
            throw new ArgumentNotValidException(
                "La palabra clave '" + newKeyword + "' ya fue agregada a este Paper (no se permiten duplicados, sin importar mayúsculas/minúsculas).");
        }
    }

    private void validateKeywordRepetitionInTitle(Congress congress, String title, String keyword) {
        if (!congress.isKeywordRepetition() && title != null && title.toLowerCase().contains(keyword.toLowerCase())) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' no permite que las palabras clave aparezcan repetidas en el título. La palabra '" + keyword + "' aparece en el título.");
        }
    }

    private void validateKeywordCountMax(Congress congress, int totalAfterOperation) {
        Integer max = congress.getMaxKeywords();
        if (max != null && totalAfterOperation > max) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' permite un máximo de " + max + " palabras clave.");
        }
    }

    private void validateKeywordCountMin(Congress congress, int totalKeywords) {
        Integer min = congress.getMinKeywords();
        if (min != null && totalKeywords < min) {
            throw new ArgumentNotValidException(
                "El congreso '" + congress.getName() + "' requiere un mínimo de " + min +
                " palabras clave para poder enviar el trabajo. Actualmente tiene " + totalKeywords + ".");
        }
    }
}