package com.tfi.gestion_congresos_backend.dtos;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationRequestDTO {
    
    @NotBlank(message = "La devolución de la evaluación no puede estar vacía")
    private String feedback;

    @NotNull(message = "La nueva fecha de cierre es obligatoria")
    @Future(message = "La fecha de inicio de inscripción debe ser una fecha futura")
    private LocalDateTime newDeadline; 
    
    @NotNull(message = "El estado de la evaluación es obligatorio")
    private PaperStatus newStatus;
}