package com.tfi.gestion_congresos_backend.dtos.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerWorkloadResponseDTO {

    private String reviewerCode;
    private String reviewerFullName;
    private String institution;
    private Long assignedPapersCount;
}