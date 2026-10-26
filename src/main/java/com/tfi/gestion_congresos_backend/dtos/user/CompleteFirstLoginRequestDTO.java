package com.tfi.gestion_congresos_backend.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteFirstLoginRequestDTO {

    @NotBlank(message = "Se requiere la contraseña actual (temporal)")
    private String currentPassword;

    @NotBlank(message = "Se requiere la nueva contraseña")
    @Size(min = 8, max = 32, message = "La contraseña debe tener entre 8 y 32 caracteres")
    private String newPassword;

    @NotBlank(message = "Se requiere confirmar la nueva contraseña")
    private String confirmPassword;
    
    @NotNull(message = "El DNI es obligatorio")
    private Long dni;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    @NotBlank(message = "La institución es obligatoria")
    private String institution;
}