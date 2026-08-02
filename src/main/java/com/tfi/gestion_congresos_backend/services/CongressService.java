package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressService {
	
	List<CongressResponseDTO> getAllCongresses();
	
	List<CongressResponseDTO> getCongressesByEnabled(boolean enabled);
	
	CongressResponseDTO getCongressById(Long congressId);
	
	CongressResponseDTO createCongress(CongressRequestDTO congressRequestDTO);
	
	void disableCongress(Long congressId);
	
	void enableCongress(Long congressId);
	
	CongressResponseDTO updateCongress(Long congressId, CongressRequestDTO congressRequestDTO);
	
	boolean existsByCongressIdAndUserIdAndRoleName(Long congressId, Long userId, RoleName role);
	
	boolean existsById(Long congressId);
}
