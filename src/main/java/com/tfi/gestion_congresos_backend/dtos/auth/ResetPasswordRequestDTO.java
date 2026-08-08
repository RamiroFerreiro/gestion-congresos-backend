package com.tfi.gestion_congresos_backend.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDTO {

    @NotBlank(message = "El Token es requerido.")
    private String token;

    @NotBlank(message = "La contraseña es requerida.")
    private String newPassword;

    @NotBlank(message = "La contraseña es requerida")
    private String confirmPassword;
}