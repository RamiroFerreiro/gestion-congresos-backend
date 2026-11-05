package com.tfi.gestion_congresos_backend.dtos.congress_participant;

import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;

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
public class CongressParticipantResponseDTO {

    private Long idCongressParticipant;
    private UserResponseDTO user;
    private RoleName role;
    private String institution;
    private boolean active;
    private LocalDateTime registrationDate;
}
