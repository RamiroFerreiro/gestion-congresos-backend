package com.tfi.gestion_congresos_backend.repository.projection;

public interface CoauthorshipPaperProjection {
    String getCode();
    String getTitle();
    String getThematicArea();
    long getAcceptedAuthorCount();
    Integer getMaxNumberOfAuthors();
}
