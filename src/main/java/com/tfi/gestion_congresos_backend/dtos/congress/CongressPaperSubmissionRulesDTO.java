package com.tfi.gestion_congresos_backend.dtos.congress;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del congreso que necesita un participante para crear o editar un trabajo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressPaperSubmissionRulesDTO {

    private Long congressId;
    private String code;
    private String name;
    private LocalDateTime presentationStartDate;
    private LocalDateTime presentationEndDate;
    private Integer maxNumberOfAuthors;
    private boolean keywordRepetition;
    private Integer minKeywords;
    private Integer maxKeywords;
    private Set<String> thematicAreas;
    private boolean enabled;
}
