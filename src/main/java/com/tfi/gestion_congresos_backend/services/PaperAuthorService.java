package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorUpdateRequestDTO;

import java.util.List;

public interface PaperAuthorService {
    
    // GETS
    List<PaperAuthorResponseDTO> getPaperAuthors(String paperCode);
    List<PaperAuthorResponseDTO> getCongressAuthors(String congressCode);

    // SOLICITUDES
    List<PaperAuthorResponseDTO> requestToJoinPaper(String paperCode, String userCode);
    List<PaperAuthorResponseDTO> acceptAuthorRequest(String paperCode, String userCode);
    List<PaperAuthorResponseDTO> rejectAuthorRequest(String paperCode, String userCode);
    
    // MODIFICACIÓN DIRECTA
    List<PaperAuthorResponseDTO> addAuthorDirectly(String paperCode, String userCode);
    List<PaperAuthorResponseDTO> removeAuthorFromPaper(String paperCode, String userCode);
    
    // ACTUALIZACIÓN EN LOTE (Bulk)
    List<PaperAuthorResponseDTO> updatePaperAuthors(String paperCode, List<PaperAuthorUpdateRequestDTO> updates);

    
}