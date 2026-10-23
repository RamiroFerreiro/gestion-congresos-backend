package com.tfi.gestion_congresos_backend.dtos.paper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperAuthorUpdateRequestDTO {

    @NotBlank(message = "El código de usuario es obligatorio")
    private String userCode;

    @NotNull(message = "El indicador isMainAuthor es obligatorio")
    private Boolean isMainAuthor;

    @NotNull(message = "El indicador isPresentator es obligatorio")
    private Boolean isPresentator;

    @NotNull(message = "El orden es obligatorio")
    @Positive(message = "El orden de autoría debe ser un número positivo")
    private Long authorOrder;
}