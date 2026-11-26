package com.tfi.gestion_congresos_backend.dtos.paper;

/** Public participant catalog: deliberately excludes reviewer, payment and author data. */
public record CoauthorshipPaperResponseDTO(
        String code, String title, String thematicArea,
        long acceptedAuthorCount, Integer maxNumberOfAuthors) {}
