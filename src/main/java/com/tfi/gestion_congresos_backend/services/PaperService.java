package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperPendingReassignmentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerWorkloadResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;

import java.util.List;
import java.util.Set;

public interface PaperService {
    // GETS
	List<PaperSummaryResponseDTO> getAssignedPapers(String congressCode, String reviewerCode);
	List<PaperPendingReassignmentResponseDTO> getPapersPendingReassignment(String congressCode);
	List<ReviewerWorkloadResponseDTO> getAvailableReviewersWithWorkload(String congressCode);
    PaperDetailResponseDTO getPaperById(Long paperId);
    Paper getPaperByPaperId(Long paperId);
    PaperDetailResponseDTO getPaperByCode(String code);
    Paper getPaperEntityByCode(String code);
    List<PaperSummaryResponseDTO> getPapersByCongressCode(String congressCode);

    // CREATE & UPDATE
    PaperDetailResponseDTO createPaper(PaperRequestDTO dto);
    PaperDetailResponseDTO updatePaper(String paperCode, PaperUpdateRequestDTO dto);

    // KEYWORDS
    Set<String> addKeywordToPaper(String paperCode, String keyword);
    Set<String> removeKeywordFromPaper(String paperCode, String keyword);

    // EVALUADORES Y SUBMIT
    MessageResponseDTO assignReviewerToPaper(String paperCode, String reviewerCode);
    MessageResponseDTO reassignReviewerToPaper(String paperCode, String newReviewerCode);
    PaperDetailResponseDTO submitPaper(String paperCode, boolean confirmDiscardPendingRequests);
}
