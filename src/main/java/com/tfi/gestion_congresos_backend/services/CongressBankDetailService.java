package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;

public interface CongressBankDetailService {

    CongressBankDetailResponseDTO create(String congressCode, CongressBankDetailRequestDTO request);

    CongressBankDetailResponseDTO getByCongressCode(String congressCode);
    
    CongressBankDetailResponseDTO update(String congressCode, CongressBankDetailRequestDTO request);

    void delete(String congressCode);

    boolean existsByCongressCode(String congressCode);
} 
