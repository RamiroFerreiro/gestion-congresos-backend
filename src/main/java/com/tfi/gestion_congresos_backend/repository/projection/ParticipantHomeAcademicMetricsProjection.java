package com.tfi.gestion_congresos_backend.repository.projection;

public interface ParticipantHomeAcademicMetricsProjection {

    long getTotalAssociatedPapers();

    long getDrafts();

    long getNeedsRevision();

    long getUnderEvaluation();
}
