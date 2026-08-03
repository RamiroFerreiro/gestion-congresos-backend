package com.tfi.gestion_congresos_backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El formato del email es inválido.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 100,
            message = "La contraseña debe tener entre 8 y 100 caracteres.")
    private String password;

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