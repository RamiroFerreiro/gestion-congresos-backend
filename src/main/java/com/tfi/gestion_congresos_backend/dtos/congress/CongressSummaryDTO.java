package com.tfi.gestion_congresos_backend.dtos.congress;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressSummaryDTO {

    private String code;
    private String name;
    private String place;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime presentationEndDate; // Fecha límite Call for Papers
    private Set<String> thematicAreas;
    
    private Boolean isFree;
    private Boolean enabled;
}
