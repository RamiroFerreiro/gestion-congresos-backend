package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.CongressRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;

public interface CongressParticipantService {
    
    MessageResponseDTO registerToCongress(String congressCode, CongressRegistrationRequestDTO requestDTO);
    MessageResponseDTO unregisterFromCongress(String targetUserCode, String congressCode);

}
