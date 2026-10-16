package com.tfi.gestion_congresos_backend.dtos.paper;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaperUpdateRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El área temática es obligatoria")
    private String thematicArea;

    @NotBlank(message = "El resumen es obligatorio")
    private String summary;

    @NotNull(message = "La fecha de presentación es obligatoria")
    @Future(message = "La fecha de presentación debe ser futura")
    private LocalDateTime presentationDate;

    @NotNull(message = "El tipo de trabajo es obligatorio")
    private Long congressPaperTypeId;
}