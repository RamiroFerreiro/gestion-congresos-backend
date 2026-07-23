package com.tfi.gestion_congresos_backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequestDTO {

    private String firstName;
    private String lastName;
    private String email;
    private Long dni;
    private String institution;
    private String country;
    private Long roleId;

}