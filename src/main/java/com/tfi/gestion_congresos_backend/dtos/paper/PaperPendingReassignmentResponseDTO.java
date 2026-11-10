package com.tfi.gestion_congresos_backend.dtos.paper;

import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperPendingReassignmentResponseDTO {

    private String code;
    private String title;
    private String thematicArea;
    private String version;
    private PaperStatus status;
    private LocalDateTime presentationDate;

    private String congressCode;
    private String congressName;

    private String previousReviewerCode;
    private String previousReviewerFullName;
}