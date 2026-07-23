package com.tfi.gestion_congresos_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Long dni;
    private String institution;
    private String country;
    private Long roleId;
}