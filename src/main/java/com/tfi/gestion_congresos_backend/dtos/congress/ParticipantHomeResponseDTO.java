package com.tfi.gestion_congresos_backend.dtos.congress;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantHomeResponseDTO {

    private String congressCode;
    private String congressName;
    private String place;
    private LocalDateTime presentationEndDate;
    private long totalAssociatedPapers;
    private long drafts;
    private long needsRevision;
    private long underEvaluation;
    private long paymentsWithoutValidReceipt;
    private long paymentsPendingApproval;
    private long paymentsVerified;
}
