package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressService {
	
	List<CongressResponseDTO> getAllCongresses();
	
	List<CongressResponseDTO> getCongressesByEnabled(boolean enabled);
	
	CongressResponseDTO getCongressById(Long congressId);
	
	Congress getCongressByCongressId(Long congressId);
	
	CongressResponseDTO createCongress(CongressRequestDTO congressRequestDTO);
	
	MessageResponseDTO disableCongress(Long congressId);
	
	MessageResponseDTO enableCongress(Long congressId);
	
	CongressResponseDTO updateCongress(Long congressId, CongressRequestDTO congressRequestDTO);
	
	boolean existsByCongressIdAndUserIdAndRoleName(Long congressId, Long userId, RoleName role);
	
	boolean existsById(Long congressId);
	
	MessageResponseDTO addParticipantToCongress(Long congressId, Long participantId);
}
