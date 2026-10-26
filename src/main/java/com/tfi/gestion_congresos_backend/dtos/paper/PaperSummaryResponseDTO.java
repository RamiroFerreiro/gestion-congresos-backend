package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSummaryResponseDTO {

    private String code;
    private String title;
    private String thematicArea;
    private String version;
    private PaperStatus status;
    private LocalDateTime presentationDate;
    
    private String congressCode;
    private String congressName;
}