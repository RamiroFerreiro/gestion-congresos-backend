package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperDetailResponseDTO {

    private String code;
    private String title;
    private String thematicArea;
    private String version;
    private PaperStatus status;
    private String summary;
    private Set<String> keywords;
    private LocalDateTime presentationDate;
    private boolean pay;

    private String congressCode;
    private String congressName;

    private String reviewerCode;
    private String reviewerFullName;

    private String congressPaperTypeName;

    private List<AuthorResponseDTO> authors;
}