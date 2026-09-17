package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Paper;

import java.util.List;
import java.util.Set;

// Se deben implementar metodos CRUD
// (C): Crean un nuevo registro en la BD
// (R): Lee un registro existente en la BD
// (U): Actualiza un registro existente en la BD
// (D): Elimina un registro existente en la BD
//

public interface PaperService {

    /// Busca todos los trabajos asignados al ID de un evaluador
    List<PaperResponseDTO> getAssignedPapers(Long reviewerId);
    
    /// Obtener un paper por su ID:
    PaperResponseDTO getPaperById(Long paperId);
    
    /// Obtener una entidad paper por su ID:
    Paper getPaperByPaperId(Long paperId);

    /// Obtener todos los trabajos de un congreso por su Codigo (R)
    List<PaperResponseDTO> getPapersByCongressCode(String congressCode);

    /// Obtener un paper por su Codigo (R)
    PaperResponseDTO getPaperByCode(String code);

    /// Obtener una entidad paper por su Codigo (R)
    Paper getPaperEntityByCode(String code);

    /// Asignar un evaluador a un trabajo (C)
    MessageResponseDTO assignReviewerToPaper(String paperCode, Long reviewerId);

    // Crear paper (C)
    PaperResponseDTO createPaper(PaperRequestDTO dto);


    // Enviar paper (U)
    PaperResponseDTO submitPaper(String paperCode);
    // Crear Autor en paper (C)
    List<AuthorResponseDTO> addAuthorToPaper(String paperCode, Long userId);

    // Remover Autor en paper (D)
    List<AuthorResponseDTO> removeAuthorFromPaper(String paperCode, Long userId);
    
    // Solitar unirte a un paper (C)
    List<AuthorResponseDTO> requestToJoinPaper(String paperCode, Long userId);

    // Aceptar solicitud de union a un paper (U)
    List<AuthorResponseDTO> acceptAuthorRequest(String paperCode, Long userId);

    // Rechazar solicitud de union a un paper (U)
    List<AuthorResponseDTO> rejectAuthorRequest(String paperCode, Long userId);
    
    // Crear palabra clave en Paper (C)
    Set<String> addKeywordToPaper(String paperCode, String keyword);

    // Removerr palabra clave en Paper (R)  
    Set<String> removeKeywordFromPaper(String paperCode, String keyword);



}