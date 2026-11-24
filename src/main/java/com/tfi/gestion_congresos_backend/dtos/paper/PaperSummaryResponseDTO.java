package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.PaperType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSummaryResponseDTO {

    // Identificación y Navegación
    private String code;
    private String title;
    private String thematicArea;
    private String version;

    // Tipo de Trabajo
    private PaperType congressPaperType;

    // Estado Académico y Asignación
    private PaperStatus status;
    private String congressCode;
    private String congressName;
    private String reviewerName;
    
    // Estado Financiero
    private boolean pay;

    // Fechas y Autoría
    private LocalDateTime submittedAt;
}
