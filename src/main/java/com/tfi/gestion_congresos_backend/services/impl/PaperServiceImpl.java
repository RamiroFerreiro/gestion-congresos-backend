package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.User;
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
}