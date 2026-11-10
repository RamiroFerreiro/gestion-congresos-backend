package com.tfi.gestion_congresos_backend.repository.projection;

public interface FinancialMetricsProjection {
    Long getTotalPayments();
    Long getPendingApprovalPayments();
    Long getApprovedPayments();
    Long getRejectedPayments();
}
