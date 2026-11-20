package com.tfi.gestion_congresos_backend.dtos.paper;

import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.ParticipantPaperActivityType;
import com.tfi.gestion_congresos_backend.enums.ParticipantPaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantPaperSummaryResponseDTO {
    private String code;
    private String title;
    private String version;
    private PaperStatus status;
    private LocalDateTime submittedAt;
    private boolean responsibleAuthor;
    private ParticipantPaymentStatus paymentStatus;
    private boolean canUploadPayment;
    private ParticipantPaperActivityType lastActivityType;
    private LocalDateTime lastActivityAt;
}
