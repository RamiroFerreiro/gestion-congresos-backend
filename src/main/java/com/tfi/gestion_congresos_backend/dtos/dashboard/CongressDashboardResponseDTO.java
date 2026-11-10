package com.tfi.gestion_congresos_backend.dtos.dashboard;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressDashboardResponseDTO {

    private String congressCode;
    private String congressName;
    
    private AcademicDashboardMetricsDTO academicMetrics;
    private FinancialDashboardMetricsDTO financialMetrics;
}
