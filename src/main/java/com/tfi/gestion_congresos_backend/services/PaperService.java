package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperReviewerResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;

import java.util.List;
import java.util.Set;

// Se deben implementar metodos CRUD

public interface PaperService {

///----------------------------------------------------------GETS----------------------------------------------------------///

    List<PaperSummaryResponseDTO> getAssignedPapers(String reviewerCode);

    PaperDetailResponseDTO getPaperById(Long paperId);

    Paper getPaperByPaperId(Long paperId);

    PaperDetailResponseDTO getPaperByCode(String code);

    // PaperReviewerResponseDTO getPaperForReviewer(String paperCode, String reviewerCode);

    Paper getPaperEntityByCode(String code);

    List<PaperSummaryResponseDTO> getPapersByCongressCode(String congressCode);

    ///----------------------------------------------------------CREATE & UPDATE----------------------------------------------------------///

    PaperDetailResponseDTO createPaper(PaperRequestDTO dto);

    // PaperDetailResponseDTO updatePaper(String paperCode, PaperUpdateRequestDTO dto);

    ///----------------------------------------------------------AUTHORS----------------------------------------------------------///

    List<AuthorResponseDTO> requestToJoinPaper(String paperCode, String userCode);

    List<AuthorResponseDTO> acceptAuthorRequest(String paperCode, String userCode);

    List<AuthorResponseDTO> rejectAuthorRequest(String paperCode, String userCode);

    List<AuthorResponseDTO> addAuthorToPaper(String paperCode, String userCode);

    List<AuthorResponseDTO> removeAuthorFromPaper(String paperCode, String userCode);

    MessageResponseDTO assignReviewerToPaper(String paperCode, String reviewerCode);

    ///----------------------------------------------------------KEYWORDS----------------------------------------------------------///

    Set<String> addKeywordToPaper(String paperCode, String keyword);

    Set<String> removeKeywordFromPaper(String paperCode, String keyword);

    ///----------------------------------------------------------SUBMIT----------------------------------------------------------///

    PaperDetailResponseDTO submitPaper(String paperCode);

}