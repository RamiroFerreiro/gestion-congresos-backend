package com.tfi.gestion_congresos_backend.dtos.paper;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewerInboxResponseDTO {
    private String congressCode;
    private String congressName;
    private int totalAssigned;
    private List<ReviewerInboxPaperDTO> papers;
}
