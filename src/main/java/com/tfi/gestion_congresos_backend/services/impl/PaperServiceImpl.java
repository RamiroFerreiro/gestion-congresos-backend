package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperMapper;
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

    private static final int CODE_LENGTH = 6;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponseDTO> getAssignedPapers(Long reviewerId) {
        if (!userService.existsById(reviewerId)) {
            throw new ResourceNotFoundException("Evaluador no encontrado con el ID: " + reviewerId);
        }
        List<Paper> assignedPapers = paperRepository.findByUserReviewer_UserId(reviewerId);
        return paperMapper.toPaperResponseDTOList(assignedPapers);
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponseDTO> getPapersByCongressId(Long congressId) {
        if (!congressService.existsById(congressId)) {
            throw new ResourceNotFoundException("Congreso no encontrado con ID: " + congressId);
        }
        List<Paper> papers = paperRepository.findByCongressIdWithDetails(congressId);
        return papers.stream().map(paperMapper::toPaperResponseDTO).toList();
    }

    ///----------------------------------------------------------CREATE----------------------------------------------------------///

    @Override
    @Transactional
    public PaperResponseDTO createPaper(PaperRequestDTO dto) {

        Congress congress = congressService.getCongressByCongressId(dto.getCongressId());

        validateCongressEnabled(congress);
        validateThematicArea(congress, dto.getThematicArea());
        validatePresentationDateWithinWindow(congress, dto.getPresentationDate());
        validateMaxAuthors(congress, dto.getAuthorUserIds().size());

        Paper paper = paperMapper.toEntity(dto);
        paper.setCode(generatePaperCode());
        paper.setVersion("1.0");
        paper.setStatus(PaperStatus.NOT_SUBMITTED);
        paper.setCongress(congress);
        paper.setAuthors(new ArrayList<>());
        paper.setKeywords(new HashSet<>());

        // Todo se arma EN MEMORIA, sobre el mismo objeto paper — nada se persiste todavía
        for (Long authorId : dto.getAuthorUserIds()) {
            User author = userService.getUserByUserId(authorId);
            addAuthorInternal(paper, author);
        }

        for (String keyword : dto.getKeywords()){
            addKeywordInternal(paper,keyword);
        }

        // UN SOLO save al final: cascade=ALL inserta Paper + PaperAuthors + keywords juntos
        Paper savedPaper = paperRepository.save(paper);

        return paperMapper.toPaperResponseDTO(savedPaper);
    }

    ///----------------------------------------------------------AUTHORS----------------------------------------------------------///

    @Override
    @Transactional
    public List<AuthorResponseDTO> addAuthorToPaper(Long paperId, Long userId) {
        Paper paper = getPaperByPaperId(paperId);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        User user = userService.getUserByUserId(userId);
        addAuthorInternal(paper, user);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> removeAuthorFromPaper(Long paperId, Long userId) {
        Paper paper = getPaperByPaperId(paperId);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        removeAuthorInternal(paper, userId);

        Paper savedPaper = paperRepository.save(paper);
        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

    @Override
    @Transactional
    public MessageResponseDTO assignReviewerToPaper(Long paperId, Long reviewerId) {
        Paper paper = getPaperByPaperId(paperId);

        User paperReviewer = paper.getUserReviewer();
        if (paperReviewer != null) {
            if (!paperReviewer.getUserId().equals(reviewerId)) {
                throw new ResourceAlreadyExistsException(
                    "El trabajo con ID " + paperId + " ya tiene asignado un evaluador (" +
                    paperReviewer.getFirstName() + " " + paperReviewer.getLastName() + ")."
                );
            } else {
                throw new ResourceAlreadyExistsException(
                    "El evaluador con ID " + reviewerId + " ya está asignado a este trabajo."
                );
            }
        }

        User reviewer = userService.getUserByUserId(reviewerId);
        Congress congress = paper.getCongress();

        boolean isParticipantInCongress = congressService.existsByCongressIdAndUserIdAndRoleName(
                congress.getCongressId(), reviewerId, RoleName.EVALUATOR);
        if (!isParticipantInCongress) {
            throw new ArgumentNotValidException(
                "El evaluador con ID " + reviewerId + " no pertenece al mismo congreso que el trabajo con ID " + paperId + ".");
        }

        paper.setUserReviewer(reviewer);
        paperRepository.save(paper);

        return new MessageResponseDTO("Evaluador asignado con éxito");
    }

    ///----------------------------------------------------------KEYWORDS----------------------------------------------------------///

    @Override
    @Transactional
    public Set<String> addKeywordToPaper(Long paperId, String keyword) {
        Paper paper = getPaperByPaperId(paperId);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        addKeywordInternal(paper, keyword);

        Paper savedPaper = paperRepository.save(paper);
        return savedPaper.getKeywords();
    }

    @Override
    @Transactional
    public Set<String> removeKeywordFromPaper(Long paperId, String keyword) {
        Paper paper = getPaperByPaperId(paperId);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        removeKeywordInternal(paper, keyword);

        Paper savedPaper = paperRepository.save(paper);
        return savedPaper.getKeywords();
    }

    ///----------------------------------------------------------SUBMIT----------------------------------------------------------///

    @Override
    @Transactional
    public PaperResponseDTO submitPaper(Long paperId) {
        Paper paper = getPaperByPaperId(paperId);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            throw new ArgumentNotValidException("El Paper debe tener al menos un autor antes de enviarlo a revisión");
        }

        validateKeywordCountMin(paper.getCongress(), paper.getKeywords().size());

        paper.setStatus(PaperStatus.UNDER_EVALUATION);
        Paper saved = paperRepository.save(paper);
        return paperMapper.toPaperResponseDTO(saved);
    }

    ///----------------------------------------------------------LÓGICA INTERNA (sin @Transactional, sin fetch, sin save)----------------------------------------------------------///
    // Estos métodos SOLO operan sobre un Paper ya cargado en memoria. No buscan nada en la DB,
    // no abren transacción propia, no guardan. Los llama tanto createPaper (varias veces, en memoria,
    // antes del primer save) como los endpoints públicos de add/remove (una vez, después de fetchear).

    private void addAuthorInternal(Paper paper, User user) {
        Congress congress = paper.getCongress();

        validateMaxAuthors(congress, paper.getAuthors().size() + 1);

        boolean isAuthorInCongress = congressService.existsByCongressIdAndUserIdAndRoleName(
                congress.getCongressId(), user.getUserId(), RoleName.EXPOSITOR);
        if (!isAuthorInCongress && user.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new ArgumentNotValidException(
                "El usuario con ID " + user.getUserId() + " no está inscripto como autor en el congreso con ID " + congress.getCongressId());
        }

        boolean alreadyAuthor = paper.getAuthors().stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(user.getUserId()));
        if (alreadyAuthor) {
            throw new ResourceAlreadyExistsException(
                "El usuario con ID " + user.getUserId() + " ya es autor de este Paper");
        }

        int nextOrder = paper.getAuthors().size() + 1;
        PaperAuthor newPaperAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(user)
                .authorOrder(nextOrder)
                .build();

        paper.getAuthors().add(newPaperAuthor);
    }

    private void removeAuthorInternal(Paper paper, Long userId) {
        PaperAuthor toRemove = paper.getAuthors().stream()
                .filter(pa -> pa.getAuthor().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "El usuario con ID " + userId + " no es autor de este Paper"));

        if (toRemove.getAuthorOrder() == 1) {
            throw new ArgumentNotValidException(
                "No se puede eliminar al autor creador del Paper. El Paper debe tener siempre al menos un autor.");
        }

        paper.getAuthors().remove(toRemove);

        List<PaperAuthor> remaining = paper.getAuthors().stream()
                .sorted(Comparator.comparingInt(PaperAuthor::getAuthorOrder))
                .toList();

        int order = 1;
        for (PaperAuthor pa : remaining) {
            pa.setAuthorOrder(order++);
        }
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

    ///----------------------------------------------------------VALIDACIONES / HELPERS----------------------------------------------------------///

    private void validatePaperEditableState(Paper paper, PaperStatus... allowedStates) {
        boolean isAllowed = Arrays.stream(allowedStates).anyMatch(s -> s == paper.getStatus());
        if (!isAllowed) {
            throw new ArgumentNotValidException(
                "No se puede realizar esta acción con el Paper en estado " + paper.getStatus() +
                ". Estados permitidos: " + Arrays.toString(allowedStates));
        }
    }

    private void validateMaxAuthors(Congress congress, int totalAuthorsAfterOperation) {
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