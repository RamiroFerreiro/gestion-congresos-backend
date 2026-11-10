package com.tfi.gestion_congresos_backend.dtos.dashboard;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialDashboardMetricsDTO {

    private long totalPayments;                   // Total de comprobantes registrados (100%)
    
    private MetricValueDTO pendingApprovalPayments;       // Pagos pendientes de revisión
    private MetricValueDTO approvedPayments;      // Pagos verificados/aprobados
    private MetricValueDTO rejectedPayments;      // Pagos rechazados
}
