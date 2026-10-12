package com.tfi.gestion_congresos_backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressRegistrationRequestDTO {

    @NotBlank(message = "La institución es obligatoria.")
    private String institution;
}