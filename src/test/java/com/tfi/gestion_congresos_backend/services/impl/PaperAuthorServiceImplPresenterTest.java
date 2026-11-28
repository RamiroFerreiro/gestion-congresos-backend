package com.tfi.gestion_congresos_backend.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.entities.*;
import com.tfi.gestion_congresos_backend.enums.*;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.mapper.PaperAuthorMapper;
import com.tfi.gestion_congresos_backend.repository.*;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.*;
import com.tfi.gestion_congresos_backend.services.validation.*;

@ExtendWith(MockitoExtension.class)
class PaperAuthorServiceImplPresenterTest {
    @Mock PaperAuthorRepository authors;
    @Mock PaperRepository papers;
    @Mock CongressParticipantRepository participants;
    @Mock UserService users;
    @Mock AuthorizationService authorization;
    @Mock CongressParticipantService participantService;
    @Mock CongressParticipantInstitutionValidator institution;
    @Mock SecurityEvaluator security;
    @Mock PaperAuthorMapper mapper;
    @Mock EmailService email;
    @Mock CoauthorshipPolicy policy;
    @Mock CongressService congressService;
    @InjectMocks PaperAuthorServiceImpl service;

    Paper paper;
    User main;
    PaperAuthor responsible, other, pending;

    @BeforeEach void setup() {
        paper = Paper.builder().code("P").status(PaperStatus.NOT_SUBMITTED)
                .congress(Congress.builder().code("C").build()).build();
        main = User.builder().userId(1L).code("main").enabled(true).build();
        responsible = author(main, true, true, PaperAuthorStatus.ACCEPTED, 1L);
        other = author(User.builder().userId(2L).code("other").build(), false, false, PaperAuthorStatus.ACCEPTED, 2L);
        pending = author(User.builder().userId(3L).code("pending").build(), false, true, PaperAuthorStatus.PENDING, null);
        lenient().when(papers.findByCodeForUpdate("P")).thenReturn(Optional.of(paper));
        lenient().when(authorization.getAuthenticatedUserEntity()).thenReturn(main);
        lenient().when(authors.findByPaper_CodeOrderByAuthorOrderAsc("P")).thenReturn(List.of(responsible, other, pending));
        lenient().when(authors.findByPaper_CodeAndIsMainAuthorTrue("P")).thenReturn(Optional.of(responsible));
        lenient().when(authors.findByPaper_CodeAndAuthor_Code("P", "other")).thenReturn(Optional.of(other));
        lenient().when(authors.saveAll(any())).thenAnswer(inv -> new ArrayList<>((List<PaperAuthor>) inv.getArgument(0)));
    }

    private PaperAuthor author(User user, boolean mainAuthor, boolean presenter, PaperAuthorStatus status, Long order) {
        return PaperAuthor.builder().paper(paper).author(user).isMainAuthor(mainAuthor)
                .isPresentator(presenter).status(status).authorOrder(order).build();
    }
    private List<PaperAuthorUpdateRequestDTO> updates(boolean first, boolean second) {
        return List.of(new PaperAuthorUpdateRequestDTO("main", first, 1L),
                new PaperAuthorUpdateRequestDTO("other", second, 2L));
    }

    @Test void rejectsAllUnmarkedEvenWhenPendingIsMarkedAndDoesNotMutate() {
        assertThrows(ArgumentNotValidException.class, () -> service.updatePaperAuthors("P", updates(false, false)));
        assertTrue(responsible.isPresentator());
        assertFalse(other.isPresentator());
        assertEquals  (2L, other.getAuthorOrder());
        verify(authors, never()).saveAll(any());
        verifyNoInteractions(email);
    }

    @Test void allowsOtherPresenterWithoutResponsible() {
        service.updatePaperAuthors("P", updates(false, true));
        assertFalse(responsible.isPresentator());
        assertTrue(other.isPresentator());
    }

    @Test void allowsMultiplePresenters() {
        service.updatePaperAuthors("P", updates(true, true));
        assertTrue(responsible.isPresentator());
        assertTrue(other.isPresentator());
    }

    @Test void rejectsRemovingLastPresenterEvenIfPendingIsMarked() {
        responsible.setPresentator(false);
        other.setPresentator(true);
        assertThrows(ArgumentNotValidException.class, () -> service.removeAuthorFromPaper("P", "other"));
        assertEquals(2L, other.getAuthorOrder());
        verify(authors, never()).delete(any());
        verify(authors, never()).saveAll(any());
        verifyNoInteractions(email);
    }

    @Test void allowsRemovingPresenterWhenAnotherRemains() {
        other.setPresentator(true);
        service.removeAuthorFromPaper("P", "other");
        verify(authors).delete(other);
        assertTrue(responsible.isPresentator());
    }

    @Test void rejectsChangesAfterSubmission() {
        paper.setStatus(PaperStatus.NEEDS_REVISION);
        assertThrows(ArgumentNotValidException.class, () -> service.updatePaperAuthors("P", updates(true, true)));
        assertThrows(ArgumentNotValidException.class, () -> service.removeAuthorFromPaper("P", "other"));
        verify(authors, never()).saveAll(any());
        verify(authors, never()).delete(any());
    }

    @Test void rejectsUnauthorizedChanges() {
        when(authorization.getAuthenticatedUserEntity()).thenReturn(User.builder().userId(99L).enabled(true).build());
        assertThrows(AccessDeniedException.class, () -> service.updatePaperAuthors("P", updates(true, true)));
        assertThrows(AccessDeniedException.class, () -> service.removeAuthorFromPaper("P", "other"));
        verify(authors, never()).saveAll(any());
        verify(authors, never()).delete(any());
    }
}
