package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;

public interface CongressService {
	
	List<CongressResponseDTO> getAllCongresses();
	
	List<CongressResponseDTO> getCongressesByEnabled(boolean enabled);
	
	CongressResponseDTO getCongressDTOByCode(String code);
	
	CongressResponseDTO createCongress(CongressRequestDTO congressRequestDTO);
	
	CongressResponseDTO updateCongress(String code, CongressRequestDTO congressRequestDTO);
	
	MessageResponseDTO disableCongress(String code);
	
	MessageResponseDTO enableCongress(String code);
	
	Congress getCongressEntityByCode(String code);
	
}
