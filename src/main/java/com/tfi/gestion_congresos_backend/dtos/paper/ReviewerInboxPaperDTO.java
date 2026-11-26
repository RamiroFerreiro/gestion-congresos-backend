package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.ReviewerInboxActivity;
import com.tfi.gestion_congresos_backend.enums.ReviewerInboxStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewerInboxPaperDTO {
    private String code;
    private String title;
    private String thematicArea;
    private String version;
    private ReviewerInboxStatus status;
    private ReviewerInboxActivity lastActivity;
    private LocalDateTime lastActivityAt;
}
