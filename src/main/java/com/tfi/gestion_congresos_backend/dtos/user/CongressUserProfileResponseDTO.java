package com.tfi.gestion_congresos_backend.dtos.user;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressUserProfileResponseDTO {

    // Reutilización del perfil base del usuario
    private UserResponseDTO user;

    // Datos contextuales del usuario dentro del congreso (tabla intermedia)
    private String role;            // Nombre/descripción del rol (ej: "AUTOR", "EVALUADOR")
    private String institution;     // Institución cargada para este congreso
    private LocalDateTime registrationDate; // Fecha de inscripción al congreso
}
