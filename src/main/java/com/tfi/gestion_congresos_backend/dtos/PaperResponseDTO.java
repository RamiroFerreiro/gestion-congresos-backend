package com.tfi.gestion_congresos_backend.dtos;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperResponseDTO {

    private Long paperId;
    private String title;
    private String code;
    private String thematicArea;
    private String version;
    private PaperStatus status;
    private String summary;
    private Set<String> keywords;
    private LocalDateTime presentationDate;
    
    private Long congressId;
    private String congressName;
    
    private Long reviewerId;
    private String reviewerFullName;
    
    private List<AuthorResponseDTO> authors;
}