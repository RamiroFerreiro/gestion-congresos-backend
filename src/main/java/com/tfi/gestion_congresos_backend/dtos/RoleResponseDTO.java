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
public class RoleResponseDTO {
    
    private Long roleId;
    private RoleName name;
    
}
