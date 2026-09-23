package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;

public interface CongressBankDetailService {

    CongressBankDetailResponseDTO create(Long congressId, CongressBankDetailRequestDTO request);

    CongressBankDetailResponseDTO getByCongressId(Long congressId);
    
    CongressBankDetailResponseDTO update(Long congressId, CongressBankDetailRequestDTO request);

    void delete(Long congressId);

    boolean existsByCongressId(Long congressId);
} 
