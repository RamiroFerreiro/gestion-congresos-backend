package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressBankDetail;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.CongressBankDetailMapper;
import com.tfi.gestion_congresos_backend.repository.CongressBankDetailRepository;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.services.CongressBankDetailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressBankDetailImpl implements CongressBankDetailService{

    private final CongressBankDetailRepository bankDetailRepository;
    private final CongressRepository congressRepository;
    private final CongressBankDetailMapper bankDetailMapper;

    @Override
    public CongressBankDetailResponseDTO create(Long congressId, CongressBankDetailRequestDTO request) {

        //Validar que el congreso exista
        Congress congress = congressRepository.findById(congressId)
                .orElseThrow(() -> new ResourceNotFoundException("Congreso no encontrado con id: " + congressId));

        //Validar que el congreso sea de pago
        if (Boolean.TRUE.equals(congress.isFree())) {
            throw new ArgumentNotValidException("No se pueden registrar datos bancarios en un congreso gratuito");
        }

        //Validar que no existan datos bancarios registrados previamente
        if (bankDetailRepository.existsByCongress_CongressId(congressId)) {
            throw new ResourceAlreadyExistsException("El congreso ya cuenta con datos bancarios registrados");
        }

        //Mapear y construir la entidad
        CongressBankDetail bankDetail = bankDetailMapper.toEntity(request, congress);

        //Guardado
        CongressBankDetail savedBankDetail = bankDetailRepository.save(bankDetail);

        //Retornar respuesta DTO
        return bankDetailMapper.toCongressBankDetailResponseDTO(savedBankDetail);
        
    }
    
}
