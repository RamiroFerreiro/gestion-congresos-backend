package com.tfi.gestion_congresos_backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El formato del email es inválido.")
    private String email;

    @NotNull(message = "El DNI es obligatorio.")
    @Positive(message = "El DNI debe ser un número positivo.")
    private Long dni;

    @NotBlank(message = "La institución es obligatoria.")
    private String institution;

    @NotBlank(message = "El país es obligatorio.")
    private String country;

    @NotNull(message = "Debe seleccionar un rol.")
    private Long roleId;
}