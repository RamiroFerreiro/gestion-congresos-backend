package com.tfi.gestion_congresos_backend.dtos;


import com.tfi.gestion_congresos_backend.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRoleRequestDTO {

    @NotNull(message = "El nombre del rol no puede ser nulo")
    private RoleName roleName;
}