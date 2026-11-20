package com.tfi.gestion_congresos_backend.dtos.evaluation;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Código histórico del evaluador. Se informa solo cuando la identidad del evaluador es visible para el consumidor autenticado.", nullable = true)
    private String evaluatorCode;

    @Schema(description = "Nombre completo actual del evaluador histórico. Se informa solo cuando la identidad del evaluador es visible para el consumidor autenticado.", nullable = true)
    private String evaluatorFullName;

}
