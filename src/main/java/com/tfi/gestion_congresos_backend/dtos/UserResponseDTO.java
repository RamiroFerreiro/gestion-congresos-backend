package com.tfi.gestion_congresos_backend.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Long dni;
    private String institution;
    private String country;

}
