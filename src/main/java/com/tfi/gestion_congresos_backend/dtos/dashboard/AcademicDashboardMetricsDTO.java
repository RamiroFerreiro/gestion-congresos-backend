package com.tfi.gestion_congresos_backend.dtos.dashboard;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicDashboardMetricsDTO {

    private long totalSubmissions;                       // Total de trabajos (100%)
    
    private MetricValueDTO unassignedSubmissions;         // Sin evaluador
    private MetricValueDTO underEvaluationSubmissions;           // En evaluación
    private MetricValueDTO needsRevisionSubmissions;   // Con observaciones
    private MetricValueDTO acceptedSubmissions;           // Aceptados
    private MetricValueDTO rejectedSubmissions;           // Rechazados

    private MetricValueDTO notSubmittedSubmissions;           // Rechazados

}
