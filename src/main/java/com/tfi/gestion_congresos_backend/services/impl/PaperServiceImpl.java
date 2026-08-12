package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;
    private final CongressService congressService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    // Recibe el ID del evaluador
    public List<PaperResponseDTO> getAssignedPapers(Long reviewerId) {
    	// Validar existencia del evaluador:
        if (!userService.existsById(reviewerId)) {
            throw new ResourceNotFoundException("Evaluador no encontrado con el ID: " + reviewerId);
        }
    	
        // Consulta en la BD los papers filtrados por el ID del evaluador
        List<Paper> assignedPapers = paperRepository.findByUserReviewer_UserId(reviewerId);
        // Convierte la lista de entidades Paper a lista de DTOs y la retorna
        return paperMapper.toPaperResponseDTOList(assignedPapers);
    }
    
    @Override
    @Transactional(readOnly = true)
    /// Obtener un paper por su ID:
    public PaperResponseDTO getPaperById(Long paperId) {
    	Paper paper = paperRepository.findById(paperId).orElseThrow(() ->
        new ResourceNotFoundException("Trabajo no encontrado con ID: " + paperId));

    	PaperResponseDTO result = paperMapper.toPaperResponseDTO(paper);

    	return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    /// Obtener una entidad paper por su ID:
    public Paper getPaperByPaperId(Long paperId) {
    	Paper paper = paperRepository.findById(paperId).orElseThrow(() ->
        new ResourceNotFoundException("Trabajo no encontrado con ID: " + paperId));

    	return paper;
    }
    
    @Override
    @Transactional(readOnly = true)
    /// Obtener todos los trabajos de un congreso por su ID:
    public List<PaperResponseDTO> getPapersByCongressId(Long congressId) {
    	
    	// Validar existencia del congreso:
        if (!congressService.existsById(congressId)) {
            throw new ResourceNotFoundException("Congreso no encontrado con ID: " + congressId);
        }
    	
    	List<Paper> papers = paperRepository.findByCongressIdWithDetails(congressId);
		
		
		List<PaperResponseDTO> result = papers.stream()
				.map(paperMapper::toPaperResponseDTO)
				.toList();
		
		return result;
    }
    
    @Override
    @Transactional
    /// Asignar un evaluador a un trabajo:
    public void assignReviewerToPaper(Long paperId, Long reviewerId) {
    	// Buscar paper:
    	Paper paper = getPaperByPaperId(paperId);
    	
    	// Verificar que ya no tenga un evaluador asignado:
    	User paperReviewer = paper.getUserReviewer();
    	if (paperReviewer != null) {
    	    // Intento de asignarle otro evaluador:
    	    if (!paperReviewer.getUserId().equals(reviewerId)) {
    	        throw new ResourceAlreadyExistsException(
    	            "El trabajo con ID " + paperId + " ya tiene asignado un evaluador (" + paperReviewer.getFirstName() + " " + paperReviewer.getLastName()+ ")."
    	        );
    	    } 
    	    // Intento de asignarle exactamente el mismo:
    	    else {
    	        throw new ResourceAlreadyExistsException(
    	            "El evaluador con ID " + reviewerId + " ya está asignado a este trabajo."
    	        );
    	    }
    	}
    	
    	// Buscar evaluador:
    	User reviewer = userService.getUserByUserId(reviewerId);
    	
    	// Comparar si pertenecen al mismo congreso:
    	Congress congress = paper.getCongress();
    	
    	boolean isParticipantInCongress = congressService.existsByCongressIdAndUserIdAndRoleName(congress.getCongressId(), reviewerId, RoleName.EVALUATOR);
    	if (!isParticipantInCongress) {
    		throw new ArgumentNotValidException("El evaluador con ID " + reviewerId + " no pertenece al mismo congreso que el trabajo con ID " + paperId + ".");
    	}
    	
    	// Asignar evaluador al paper:
    	paper.setUserReviewer(reviewer);
    	paperRepository.save(paper);
    }

    @Override
    @Transactional
    public PaperResponseDTO createPaper(PaperRequestDTO dto) {

        // Validar que cada Expositor esté inscripto como tal en el congreso
        for (Long authorId : dto.getAuthorUserIds()) {
            User auth = userService.getUserByUserId(authorId);
            boolean isAuthorInCongress = congressService.existsByCongressIdAndUserIdAndRoleName(
                    dto.getCongressId(), authorId, auth.getRole().getName());
            if (!isAuthorInCongress) {
                throw new ArgumentNotValidException(
                    "El usuario con ID " + authorId + " no está inscripto como Expositor en el congreso con ID " + dto.getCongressId());
            }
        }

        Congress congress = congressService.getCongressByCongressId(dto.getCongressId());

        Paper paper = paperMapper.toEntity(dto);
        paper.setVersion("1.0");
        paper.setStatus(PaperStatus.NOT_SUBMITTED);
        paper.setCongress(congress);

        Paper savedPaper = paperRepository.save(paper);

        // Resolver autores y armar PaperAuthor con orden
        List<PaperAuthor> authors = new ArrayList<>();
        int order = 1;
        for (Long authorId : dto.getAuthorUserIds()) {
            User author = userService.getUserByUserId(authorId);
            authors.add(PaperAuthor.builder()
                    .paper(savedPaper)
                    .author(author)
                    .authorOrder(order++)
                    .build());
        }
        savedPaper.setAuthors(authors);

        Paper finalPaper = paperRepository.save(savedPaper);
        return paperMapper.toPaperResponseDTO(finalPaper);
    }

    @Override
    @Transactional
    public PaperResponseDTO submitPaper(Long paperId) {
        Paper paper = getPaperByPaperId(paperId); 

        if (paper.getStatus() != PaperStatus.NOT_SUBMITTED
                && paper.getStatus() != PaperStatus.NEEDS_REVISION) {
            throw new ArgumentNotValidException(
                "No se puede enviar un Paper en estado " + paper.getStatus() +
                ". Solo se permite desde NOT_SUBMITTED o NEEDS_REVISION.");
        }

        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            throw new ArgumentNotValidException("El Paper debe tener al menos un autor antes de enviarlo a revisión");
        }

        paper.setStatus(PaperStatus.UNDER_EVALUATION);
        Paper saved = paperRepository.save(paper);
        return paperMapper.toPaperResponseDTO(saved);
    }

    @Override
    @Transactional
    public List<AuthorResponseDTO> addAuthorToPaper(Long paperId, Long userId) {

        Paper paper = getPaperByPaperId(paperId); 

        // Regla de negocio: solo se puede agregar autores antes del primer envío
        if (paper.getStatus() != PaperStatus.NOT_SUBMITTED) {
            throw new ArgumentNotValidException(
                "No se pueden agregar autores a un Paper en estado " + paper.getStatus() +
                ". Solo se permite en NOT_SUBMITTED.");
        }

        User user = userService.getUserByUserId(userId);

        // Validar que el usuario tenga rol AUTHOR en el congreso de este Paper
        Long congressId = paper.getCongress().getCongressId();
        boolean isAuthorInCongress = congressService.existsByCongressIdAndUserIdAndRoleName(
                congressId, userId, RoleName.EXPOSITOR);
        
        // En caso que sea admin se puede inscribir igualmente
        if (!isAuthorInCongress && user.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new ArgumentNotValidException(
                "El usuario con ID " + userId + " no está inscripto como autor en el congreso con ID " + congressId);
        }

        // Evitar duplicados: que no sea ya autor de este mismo Paper
        boolean alreadyAuthor = paper.getAuthors().stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(userId));
        if (alreadyAuthor) {
            throw new ResourceAlreadyExistsException(
                "El usuario con ID " + userId + " ya es autor de este Paper");
        }

        // Calcular el próximo orden (siguiente al último existente)
        int nextOrder = paper.getAuthors().size() + 1;

        PaperAuthor newPaperAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(user)
                .authorOrder(nextOrder)
                .build();

        paper.getAuthors().add(newPaperAuthor); // gracias al cascade = ALL de Paper, se persiste solo

        Paper savedPaper = paperRepository.save(paper);

        return paperMapper.toAuthorResponseDTOList(savedPaper.getAuthors());
    }

}