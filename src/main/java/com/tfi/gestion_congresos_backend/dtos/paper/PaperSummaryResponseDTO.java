package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
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
    private String congressPaperType; // Ej: "POSTER"

    // Estado Académico y Asignación
    private PaperStatus status;
    private String reviewerName;
    
    // Estado Financiero
    private boolean pay;

    // Fechas y Autoría
    private LocalDateTime presentationDate;
}
