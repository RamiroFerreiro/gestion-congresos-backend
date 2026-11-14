package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaperFilterDTO {

    private PaperStatus status;       // Filtrar por estado puntual (IN_REVIEW, ACCEPTED, etc.)
    private Boolean unassigned;       // true = sin evaluador (users_reviewer_id IS NULL)
    private Boolean pay;              // true/false para estado de pago
    private String thematicArea;      // Filtrar por área temática
    private String search;            // Búsqueda por coincidencia en título o código
}