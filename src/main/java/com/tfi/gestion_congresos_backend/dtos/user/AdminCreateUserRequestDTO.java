package com.tfi.gestion_congresos_backend.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateUserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El formato del email es inválido.")
    private String email;

    @NotNull(message = "Debe seleccionar un rol.")
    private Long roleId;
}