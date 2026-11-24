package com.tfi.gestion_congresos_backend.dtos.paper;

import jakarta.validation.constraints.*;
import lombok.*;

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

}
