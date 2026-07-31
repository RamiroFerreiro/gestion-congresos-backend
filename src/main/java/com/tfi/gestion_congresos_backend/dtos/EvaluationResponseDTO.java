package com.tfi.gestion_congresos_backend.dtos;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationResponseDTO {
    

    // private Long evaluationId; 
    private String feedback;
    private LocalDateTime evaluationDate;
    private String evaluatedVersion; 
    private LocalDateTime newDeadline;
    private PaperStatus newStatus;

}