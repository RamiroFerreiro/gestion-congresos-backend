package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.mapper.PaperMapper;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.services.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;

    @Override
    @Transactional(readOnly = true)
    // Recibe el ID del evaluador
    public List<PaperResponseDTO> getAssignedPapers(Long reviewerId) {
        // Consulta en la BD los papers filtrados por el ID del evaluador
        List<Paper> assignedPapers = paperRepository.findByUserReviewer_UserId(reviewerId);
        // Convierte la lista de entidades Paper a lista de DTOs y la retorna
        return paperMapper.toPaperResponseDTOList(assignedPapers);
    }

}