package com.tfi.gestion_congresos_backend.dtos.congress_participant;

import com.tfi.gestion_congresos_backend.enums.RoleName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminParticipantRegistrationRequestDTO {

	@NotBlank(message = "El código del usuario es obligatorio.")
    private String userCode;

    @NotNull(message = "El rol es obligatorio.")
    private RoleName role;
}
