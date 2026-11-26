package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.paper.ReviewerInboxResponseDTO;
import com.tfi.gestion_congresos_backend.enums.ReviewerInboxStatus;

public interface ReviewerInboxService {
    ReviewerInboxResponseDTO getMyInbox(String congressCode, ReviewerInboxStatus status);
}
