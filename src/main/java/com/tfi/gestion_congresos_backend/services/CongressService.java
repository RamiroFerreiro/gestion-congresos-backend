package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressService {
	
	List<CongressResponseDTO> getAllCongresses();
	
	List<CongressResponseDTO> getCongressesByEnabled(boolean enabled);
	
	CongressResponseDTO getCongressById(Long congressId);
	
	Congress getCongressByCongressId(Long congressId);
	
	CongressResponseDTO createCongress(CongressRequestDTO congressRequestDTO);
	
	MessageResponseDTO disableCongress(String code);
	
	MessageResponseDTO enableCongress(String code);
	
	CongressResponseDTO updateCongress(String code, CongressRequestDTO congressRequestDTO);
	
	boolean existsByCongressIdAndUserIdAndRoleName(Long congressId, Long userId, RoleName role);
	
	boolean existsById(Long congressId);
	
	MessageResponseDTO addParticipantToCongress(String code, String participantCode);

	Congress getCongressByCode(String code);
	
	CongressResponseDTO getCongressDTOByCode(String code);

	boolean existsByCode(String code);
	
	boolean existsByCongressCodeAndUserCodeAndRoleName(String congressCode, String userCode, RoleName role);
}
