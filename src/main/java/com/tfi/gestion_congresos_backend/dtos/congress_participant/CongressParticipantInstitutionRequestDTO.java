package com.tfi.gestion_congresos_backend.dtos.congress_participant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressParticipantInstitutionRequestDTO {

	@NotBlank(message = "La institución es obligatoria.")
    @Size(max = 255, message = "La institución no puede superar los 255 caracteres.")
    private String institution;
}