package com.tfi.gestion_congresos_backend.services.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookAuthorDTO;
import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookPaperDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AbstractBookService;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.utils.AbstractBookWordGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AbstractBookServiceImpl implements AbstractBookService {

    private final CongressService congressService;
    private final AuthorizationService authorizationService;
    private final SecurityEvaluator securityEvaluator;
    private final PaperRepository paperRepository;
    private final CongressParticipantRepository participantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AbstractBookPaperDTO> getEligiblePapers(String congressCode) {
        Congress congress = congressService.getCongressEntityByCode(congressCode);
        return loadEligiblePapers(congress);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateAbstractBookWord(String congressCode) {
        Congress congress = congressService.getCongressEntityByCode(congressCode);
        List<AbstractBookPaperDTO> papers = loadEligiblePapers(congress);
        return AbstractBookWordGenerator.generate(congress, papers);
    }

    private List<AbstractBookPaperDTO> loadEligiblePapers(Congress congress) {
        assertCongressAdmin(congress);

        List<Paper> eligiblePapers = paperRepository.findEligibleForAbstractBook(congress.getCongressId()).stream()
                .sorted(Comparator
                        .comparing(Paper::getThematicArea, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Paper::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Paper::getCode))
                .toList();

        if (eligiblePapers.isEmpty()) {
            return List.of();
        }

        Map<Long, String> institutionsByUserId = participantRepository
                .findByCongress_CongressId(congress.getCongressId())
                .stream()
                .filter(participant -> participant.getUser() != null && participant.getInstitution() != null)
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getUserId(),
                        CongressParticipant::getInstitution,
                        (first, ignored) -> first
                ));

        return eligiblePapers.stream()
                .map(paper -> toPaperDto(paper, institutionsByUserId))
                .toList();
    }

    private AbstractBookPaperDTO toPaperDto(Paper paper, Map<Long, String> institutionsByUserId) {
        List<String> keywords = paper.getKeywords() == null
                ? List.of()
                : paper.getKeywords().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();

        return AbstractBookPaperDTO.builder()
                .code(paper.getCode())
                .title(paper.getTitle())
                .thematicArea(paper.getThematicArea())
                .summary(paper.getSummary())
                .keywords(keywords)
                .authors(toAuthorDtos(paper, institutionsByUserId))
                .build();
    }

    private List<AbstractBookAuthorDTO> toAuthorDtos(Paper paper, Map<Long, String> institutionsByUserId) {
        if (paper.getAuthors() == null) {
            return List.of();
        }

        return paper.getAuthors().stream()
                .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                .sorted(Comparator.comparing(PaperAuthor::getAuthorOrder, Comparator.nullsLast(Long::compareTo)))
                .map(paperAuthor -> {
                    User author = paperAuthor.getAuthor();
                    String institution = author == null ? null : institutionsByUserId.get(author.getUserId());
                    return AbstractBookAuthorDTO.builder()
                            .authorOrder(paperAuthor.getAuthorOrder())
                            .firstName(author == null ? null : author.getFirstName())
                            .lastName(author == null ? null : author.getLastName())
                            .institution(institution)
                            .email(author == null ? null : author.getEmail())
                            .build();
                })
                .toList();
    }

    private void assertCongressAdmin(Congress congress) {
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congress.getCode())) {
            throw new UserDisabledException("Solo el administrador del congreso puede consultar o generar el Libro de Resúmenes.");
        }
    }
}
