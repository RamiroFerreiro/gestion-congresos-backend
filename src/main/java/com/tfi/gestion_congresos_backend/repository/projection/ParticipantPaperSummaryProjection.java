package com.tfi.gestion_congresos_backend.repository.projection;

import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.ParticipantPaperActivityType;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;

public interface ParticipantPaperSummaryProjection {
    String getCode();
    String getTitle();
    String getVersion();
    PaperStatus getStatus();
    LocalDateTime getSubmittedAt();
    boolean isResponsibleAuthor();
    boolean isFreeCongress();
    PaymentStatus getRawPaymentStatus();
    ParticipantPaperActivityType getLastActivityType();
    LocalDateTime getLastActivityAt();
}
