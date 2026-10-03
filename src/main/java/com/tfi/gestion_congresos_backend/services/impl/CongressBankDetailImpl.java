package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.tfi.gestion_congresos_backend.services.CongressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressBankDetailImpl implements CongressBankDetailService{

    private final CongressBankDetailRepository bankDetailRepository;
    private final CongressService congressService;
    private final CongressBankDetailMapper bankDetailMapper;

    ///----------------------------------------------------------CREATE----------------------------------------------------------///
    
    @Transactional
    @Override
    public CongressBankDetailResponseDTO create(String congressCode, CongressBankDetailRequestDTO request) {

        //Validar que el congreso exista, sino existe lanza excepción
        Congress congress = congressService.getCongressByCode(congressCode);

        //Validar que el congreso sea de pago
        if (Boolean.TRUE.equals(congress.isFree())) {
            throw new ArgumentNotValidException("No se pueden registrar datos bancarios en un congreso gratuito");
        }

        //Validar que no existan datos bancarios registrados previamente
        if (bankDetailRepository.existsByCongress_Code(congressCode)) {
            throw new ResourceAlreadyExistsException("El congreso ya cuenta con datos bancarios registrados");
        }

        //Mapear y construir la entidad
        CongressBankDetail bankDetail = bankDetailMapper.toEntity(request, congress);

        //Guardado
        CongressBankDetail savedBankDetail = bankDetailRepository.save(bankDetail);

        //Retornar respuesta DTO
        return bankDetailMapper.toCongressBankDetailResponseDTO(savedBankDetail);
        
    }

    ///----------------------------------------------------------GET----------------------------------------------------------///
    
    @Override
    @Transactional(readOnly = true)
    public CongressBankDetailResponseDTO getByCongressCode(String congressCode) {
        CongressBankDetail bankDetail = getBankDetailEntityByCongressCode(congressCode);
        return bankDetailMapper.toCongressBankDetailResponseDTO(bankDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCongressCode(String congressCode) {
        return bankDetailRepository.existsByCongress_Code(congressCode);
    }

    ///----------------------------------------------------------UPDATE----------------------------------------------------------///
    
    @Override
    @Transactional
    public CongressBankDetailResponseDTO update(String congressCode, CongressBankDetailRequestDTO request) {

        // Buscar los datos bancarios asociados al congreso
        CongressBankDetail bankDetail = getBankDetailEntityByCongressCode(congressCode);

        // Modificar la entidad existente con los datos nuevos
        bankDetailMapper.updateEntityFromDto(request, bankDetail);

        //Guardar los cambios 
        CongressBankDetail updatedBankDetail = bankDetailRepository.save(bankDetail);

        // Devolver el DTO de respuesta
        return bankDetailMapper.toCongressBankDetailResponseDTO(updatedBankDetail);
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    
    @Override
    @Transactional
    public void delete(String congressCode) {

        //Buscar los datos bancarios asociados
        CongressBankDetail bankDetail = getBankDetailEntityByCongressCode(congressCode);
        //Eliminar la entidad
        bankDetailRepository.delete(bankDetail);
    }

    /**
     * Recupera la entidad de datos bancarios asociada al código del congreso o lanza 404.
     */
    private CongressBankDetail getBankDetailEntityByCongressCode(String congressCode) {
        return bankDetailRepository.findByCongress_Code(congressCode)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron datos bancarios para el congreso con código: " + congressCode));
    }
}


