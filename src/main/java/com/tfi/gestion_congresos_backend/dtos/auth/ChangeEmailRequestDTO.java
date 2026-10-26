package com.tfi.gestion_congresos_backend.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeEmailRequestDTO {

    @NotBlank
    @Email
    private String newEmail;

    @NotBlank
    private String currentPassword;
}