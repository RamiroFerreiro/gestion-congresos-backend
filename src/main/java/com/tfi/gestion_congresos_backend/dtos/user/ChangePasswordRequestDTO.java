package com.tfi.gestion_congresos_backend.dtos.user;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDTO {

    @NotBlank(message = "Se requiere la contraseña actual")
    private String currentPassword;

    @NotBlank(message = "Se re quiere la nueva contraseña")
    @Size(min = 8, max = 32, message = "La contraseña debe tener entre 8 y 32 carácteres")
    private String newPassword;

    @NotBlank(message = "Se requiere confimar la nueva contraseña")
    private String confirmPassword;
    
}
