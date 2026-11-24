package com.tfi.gestion_congresos_backend.repository.projection;


import java.time.LocalDateTime;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;

public interface PaperSummaryProjection {
    String getCode();
    String getTitle();
    String getThematicArea();
    String getVersion();
    PaperStatus getStatus();
    LocalDateTime getSubmittedAt();
    boolean isPay();

    //Relaciones de la Entidad Paper
    CongressSummaryProjection getCongress();
    CongressPaperTypeProjection getCongressPaperType();
    UserSimpleProjection getUserReviewer();
}
