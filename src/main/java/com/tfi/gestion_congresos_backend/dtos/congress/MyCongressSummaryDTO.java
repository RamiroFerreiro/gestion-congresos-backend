package com.tfi.gestion_congresos_backend.dtos.congress;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Summary of a congress visible from the authenticated user's congress list. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCongressSummaryDTO {

    private String code;
    private String name;
    private String place;
    private boolean enabled;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime presentationStartDate;
    private LocalDateTime presentationEndDate;
    private String role;
    private String institution;
}
