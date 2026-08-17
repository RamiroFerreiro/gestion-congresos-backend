package com.tfi.gestion_congresos_backend.dtos.auth;

import com.tfi.gestion_congresos_backend.enums.RoleName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private RoleName role;
    private String token;
}
