package com.tfi.gestion_congresos_backend.repository.projection;

public interface CongressSummaryProjection {
    String getCode();
    String getName();
    boolean isSingleBlind();
}
