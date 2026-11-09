package com.tfi.gestion_congresos_backend.dtos.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long userId;
    private String code;
    private String firstName;
    private String lastName;
    private String email;
    private Long dni;
    private String country;
    private boolean enabled;
}