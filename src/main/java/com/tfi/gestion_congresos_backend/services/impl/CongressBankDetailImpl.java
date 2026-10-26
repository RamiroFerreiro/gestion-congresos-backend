package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.context.support.BeanDefinitionDsl.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.CongressBankDetailResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressBankDetail;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.CongressBankDetailMapper;
import com.tfi.gestion_congresos_backend.repository.CongressBankDetailRepository;
import com.tfi.gestion_congresos_backend.repository.CongressRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.CongressBankDetailService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressBankDetailImpl implements CongressBankDetailService{

    private final CongressBankDetailRepository bankDetailRepository;
    private final CongressService congressService;
    private final CongressBankDetailMapper bankDetailMapper;
    private final UserService userService;
    private final SecurityEvaluator securityEvaluator;

    ///----------------------------------------------------------CREATE----------------------------------------------------------///
    
    @Transactional
    @Override
    public CongressBankDetailResponseDTO create(String congressCode, CongressBankDetailRequestDTO request) {

        //1. Obtener la entidad del usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Validar autorización que sea superAdmin o Admin del congreso
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos de administración sobre este congreso para registrar datos bancarios.");
        }
        
        //3. Validar existencia del congreso
        Congress congress = congressService.getCongressByCode(congressCode);

        //4. Validar que el congreso sea de pago
        if (Boolean.TRUE.equals(congress.isFree())) {
            throw new ArgumentNotValidException("No se pueden registrar datos bancarios en un congreso gratuito.");
        }

        //5. Validar que no existan datos bancarios cargados previamente
        if (bankDetailRepository.existsByCongress_Code(congressCode)) {
            throw new ResourceAlreadyExistsException("El congreso ya cuenta con datos bancarios registrados.");
        }

        // 6. Mapear, guardar y retornar DTO
        CongressBankDetail bankDetail = bankDetailMapper.toEntity(request, congress);
        CongressBankDetail savedBankDetail = bankDetailRepository.save(bankDetail);

        return bankDetailMapper.toCongressBankDetailResponseDTO(savedBankDetail);
    }

    ///----------------------------------------------------------GET----------------------------------------------------------///
    
    @Override
    @Transactional(readOnly = true)
    public CongressBankDetailResponseDTO getByCongressCode(String congressCode) {
        
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //1. Validar que el usuario sea Súper Admin o participe en el congreso
        if (!securityEvaluator.isParticipantOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos para ver los datos bancarios de este congreso.");
        }

        //2. Validar que el congreso exista
        Congress congress = congressService.getCongressByCode(congressCode);

        //3. Buscar los datos bancarios
        CongressBankDetail bankDetail = bankDetailRepository.findByCongress_Code(congressCode)
                .orElseThrow(() -> new ResourceNotFoundException("El congreso no tiene datos bancarios registrados."));

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

        //1. Obtener la entidad del usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Validar autorización que sea superAdmin o Admin del congreso
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos de administración sobre este congreso para actualizar datos bancarios.");
        }

        //3. Buscar los datos bancarios asociados al congreso
        CongressBankDetail bankDetail = getBankDetailEntityByCongressCode(congressCode);

        //4. Validar opcionalmente que el congreso asociado no haya mutado a gratuito
        if (Boolean.TRUE.equals(bankDetail.getCongress().isFree())) {
            throw new ArgumentNotValidException("No se pueden actualizar datos bancarios en un congreso gratuito.");
        }

        //5. Modificar la entidad existente con los datos nuevos
        bankDetailMapper.updateEntityFromDto(request, bankDetail);

        //6. Guardar los cambios    
        CongressBankDetail updatedBankDetail = bankDetailRepository.save(bankDetail);

        //7. Devolver el DTO de respuesta
        return bankDetailMapper.toCongressBankDetailResponseDTO(updatedBankDetail);
    }

    ///----------------------------------------------------------DELETE----------------------------------------------------------///
    
    @Override
    @Transactional
    public void delete(String congressCode) {

        //1. Obtener la entidad del usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Validar autorización que sea superAdmin o Admin del congreso
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("No tienes permisos de administración sobre este congreso para eliminar datos bancarios.");
        }

        //3. Buscar los datos bancarios asociados
        CongressBankDetail bankDetail = getBankDetailEntityByCongressCode(congressCode);

        //4. Eliminar la entidad
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


