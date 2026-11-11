package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.congress_participant.AdminParticipantRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantInstitutionRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressParticipantService {
    
	MessageResponseDTO registerToCongress(String congressCode, CongressParticipantInstitutionRequestDTO requestDTO);
    
    MessageResponseDTO registerParticipantByAdmin(String congressCode, AdminParticipantRegistrationRequestDTO requestDTO);

    List<CongressParticipantResponseDTO> getParticipantsByCongress(String congressCode, RoleName role);

    CongressParticipantResponseDTO getParticipantByUserCode(String congressCode, String userCode);

    MessageResponseDTO unregisterFromCongress(String targetUserCode, String congressCode);
    
    void validateUserCanBeDisabled(String userCode);
    
    MessageResponseDTO reactivateParticipantByAdmin(String congressCode, String userCode, CongressParticipantInstitutionRequestDTO requestDTO);

    void updateParticipantRole(String congressCode, String userCode, RoleName newRole);
    
    void promoteListenerToExpositor(String congressCode, String userCode);
    
    CongressParticipantResponseDTO getAuthenticatedParticipantInCongress(String congressCode);
    
    MessageResponseDTO updateAuthenticatedParticipantInstitution(String congressCode, CongressParticipantInstitutionRequestDTO requestDTO);
}
