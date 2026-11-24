package com.tfi.gestion_congresos_backend.repository.projection;

public interface AcademicMetricsProjection {
    Long getTotalSubmissions();
    Long getUnassignedSubmissions();
    Long getUnderEvaluationSubmissions();
    Long getNeedsRevisionSubmissions();
    Long getAcceptedSubmissions();
    Long getRejectedSubmissions();
    Long getNotSubmittedSubmissions();
}