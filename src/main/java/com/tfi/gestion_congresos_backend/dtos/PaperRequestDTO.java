package com.tfi.gestion_congresos_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class PaperRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El área temática es obligatoria")
    private String thematicArea;

    @NotBlank(message = "El resumen es obligatorio")
    private String summary;
    
    @NotEmpty(message = "Debe ingresar al menos una palabra clave")
    private Set<@NotBlank(message = "La palabra clave no puede estar vacía") String> keywords;

    @NotNull(message = "La fecha de presentación es obligatoria")
    @Future(message = "La fecha de presentación debe ser futura")
    private LocalDateTime presentationDate;

    @NotNull(message = "El congreso es obligatorio")
    private Long congressId;

    @NotEmpty(message = "El trabajo debe tener al menos un autor")
    private List<Long> authorUserIds;
}