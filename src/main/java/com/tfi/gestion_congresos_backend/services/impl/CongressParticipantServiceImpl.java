package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tfi.gestion_congresos_backend.dtos.congress_participant.AdminParticipantRegistrationRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantInstitutionRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress_participant.CongressParticipantResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Role;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.CongressParticipantMapper;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.repository.UserRepository;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;
import com.tfi.gestion_congresos_backend.services.CongressService;
import com.tfi.gestion_congresos_backend.services.RoleService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CongressParticipantServiceImpl implements CongressParticipantService {

	private final AuthorizationService authorizationService;
	private final RoleRepository roleRepository;
	private final CongressParticipantRepository congressParticipantRepository;
    private final CongressService congressService;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final CongressParticipantMapper congressParticipantMapper;
    private final CongressParticipantInstitutionValidator institutionValidator;
    
    @Override
    @Transactional
    public MessageResponseDTO registerToCongress(String congressCode, CongressParticipantInstitutionRequestDTO requestDTO) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Obtener el congreso
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        // 3. Validar que el congreso esté habilitado
        if (!congress.isEnabled()) {
            throw new ArgumentNotValidException(
                    "El congreso no se encuentra habilitado para recibir inscripciones."
            );
        }

        // 4. Validar que el período de inscripción esté vigente
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(congress.getRegistrationStartDate())
                || now.isAfter(congress.getRegistrationEndDate())) {
            throw new ArgumentNotValidException(
                    "El período de inscripción para este congreso no se encuentra activo."
            );
        }

        // 5. Verificar si existe una participación previa
        Optional<CongressParticipant> participantOpt =
                congressParticipantRepository.findByUserCodeAndCongressCode(
                        authenticatedUser.getCode(),
                        congressCode
                );

        if (participantOpt.isPresent()) {
            CongressParticipant participant = participantOpt.get();

            // Ya existe una inscripción activa
            if (participant.isActive()) {
                throw new ArgumentNotValidException(
                        "Ya te encuentras inscripto en este congreso."
                );
            }

            // Existe una inscripción anterior inactiva
            throw new ArgumentNotValidException(
                    "Ya existe una inscripción anterior para este congreso. "
                    + "Debés comunicarte con la administración para solicitar su reactivación."
            );
        }

        // 6. Obtener el rol LISTENER
        Role listenerRole = roleRepository.findByName(RoleName.LISTENER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol LISTENER no se encuentra configurado en el sistema."
                ));

        // 7. Crear la nueva participación
        CongressParticipant newParticipant = CongressParticipant.builder()
                .congress(congress)
                .user(authenticatedUser)
                .role(listenerRole)
                .institution(requestDTO.getInstitution().strip())
                .active(true)
                .registrationDate(now)
                .build();

        // 8. Guardar la participación
        congressParticipantRepository.save(newParticipant);

        // 9. Retornar mensaje de confirmación
        return MessageResponseDTO.builder()
                .message(
                        "Te has inscripto exitosamente al congreso '"
                        + congress.getName() + "'."
                )
                .build();
    }
    
    @Override
    @Transactional
    public MessageResponseDTO registerParticipantByAdmin(String congressCode, AdminParticipantRegistrationRequestDTO requestDTO) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Obtener el congreso
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        // 3. Validar permisos de administración sobre el congreso
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );
        
        // 4. Validar que el administrador tenga una institución definida en el congreso
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);
        
        // 5. Validar que el rol solicitado pueda asignarse
        roleService.validateAssignableRole(requestDTO.getRole());

        // 6. Buscar al usuario que se desea inscribir
        User targetUser = userRepository.findByCode(requestDTO.getUserCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un usuario con el código: " + requestDTO.getUserCode()
                ));

        // 7. Verificar si existe una participación previa
        Optional<CongressParticipant> participantOpt =
                congressParticipantRepository.findByUserCodeAndCongressCode(
                        targetUser.getCode(),
                        congressCode
                );

        if (participantOpt.isPresent()) {

            CongressParticipant participant = participantOpt.get();

            // Ya existe una inscripción activa
            if (participant.isActive()) {
                throw new ArgumentNotValidException(
                        "El usuario ya se encuentra inscripto en este congreso."
                );
            }

            // Existe una inscripción anterior inactiva
            throw new ArgumentNotValidException(
                    "El usuario tiene una inscripción anterior inactiva. "
                    + "Para volver a incorporarlo al congreso, debe utilizarse "
                    + "la operación de reactivación."
            );
        }
        
        // 8. Obtener el rol solicitado
        Role participantRole = roleRepository.findByName(requestDTO.getRole())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol solicitado no se encuentra configurado en el sistema."
                ));

        // 9. Crear la nueva participación
        CongressParticipant newParticipant = CongressParticipant.builder()
                .congress(congress)
                .user(targetUser)
                .role(participantRole)
                .institution(null)
                .active(true)
                .registrationDate(LocalDateTime.now())
                .build();

        // 10. Guardar la participación
        congressParticipantRepository.save(newParticipant);

        // 11. Retornar confirmación
        return MessageResponseDTO.builder()
                .message(
                        "Se ha inscripto exitosamente a "
                        + targetUser.getFirstName() + " "
                        + targetUser.getLastName()
                        + " en el congreso '" + congress.getName() + "'."
                )
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CongressParticipantResponseDTO> getParticipantsByCongress(String congressCode, RoleName role) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );
        
        // 4. Validar que el administrador tenga una institución definida en el congreso
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Obtener las participaciones del congreso
        List<CongressParticipant> participants =
                congressParticipantRepository.findParticipantsByCongressCodeAndRole(
                        congressCode,
                        role
                );

        // 6. Convertir las entidades a DTOs
        return participants.stream()
                .map(congressParticipantMapper::toCongressParticipantResponseDTO)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public CongressParticipantResponseDTO getParticipantByUserCode(String congressCode, String userCode) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );
        
        // 4. Validar que el administrador tenga una institución definida en el congreso
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Buscar la participación activa del usuario en el congreso
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        userCode,
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa para el usuario "
                        + userCode + " en el congreso: " + congressCode
                ));

        // 6. Convertir la entidad a DTO y retornar
        return congressParticipantMapper.toCongressParticipantResponseDTO(participant);
    }
    
    @Override
    @Transactional
    public MessageResponseDTO unregisterFromCongress(String targetUserCode, String congressCode) {

        // 1. Obtener el usuario que está realizando la acción
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        // 3. Comprobar si el usuario está intentando darse de baja a sí mismo
        boolean isSelf = authenticatedUser.getCode().equals(targetUserCode);

        // 4. Si intenta dar de baja a otra persona, validar permisos administrativos e institución definida
        if (!isSelf) {
            authorizationService.validateCongressAdminPermission(
                    authenticatedUser,
                    congressCode
            );
            
            institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);
        }

        // 5. Buscar la participación activa
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        targetUserCode,
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa para el usuario "
                        + targetUserCode + " en el congreso: " + congressCode
                ));

        // 6. Verificar que el congreso no se quede sin administradores activos
        if (participant.getRole().getName() == RoleName.ADMINISTRATOR) {

            long activeAdmins =
                    congressParticipantRepository.countActiveParticipantsByCongressCodeAndRole(
                            congressCode,
                            RoleName.ADMINISTRATOR
                    );

            if (activeAdmins <= 1) {
                throw new ArgumentNotValidException(
                        "No se puede dar de baja al último administrador activo del congreso."
                );
            }
        }

        // 7. Realizar la baja lógica y registrar quién la realizó
        LocalDateTime now = LocalDateTime.now();

        participant.setActive(false);
        participant.setDeletedAt(now);
        participant.setDeletedBy(authenticatedUser.getCode());

        // 8. Guardar los cambios
        congressParticipantRepository.save(participant);

        // 9. Retornar confirmación
        return MessageResponseDTO.builder()
                .message(
                        "La inscripción del usuario "
                        + targetUserCode
                        + " al congreso '" + congress.getName()
                        + "' ha sido dada de baja exitosamente."
                )
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateUserCanBeDisabled(String userCode) {

        // 1. Obtener las participaciones administrativas activas del usuario
        List<CongressParticipant> adminParticipations =
                congressParticipantRepository.findActiveParticipationsByUserCodeAndRole(
                        userCode,
                        RoleName.ADMINISTRATOR
                );

        // 2. Verificar que cada congreso conserve al menos un administrador habilitado
        for (CongressParticipant participation : adminParticipations) {

            String congressCode = participation.getCongress().getCode();

            long activeAdmins =
                    congressParticipantRepository.countActiveParticipantsByCongressCodeAndRole(
                            congressCode,
                            RoleName.ADMINISTRATOR
                    );

            // 3. Impedir la baja si el usuario es el único administrador habilitado
            if (activeAdmins <= 1) {
                throw new ArgumentNotValidException(
                        "No se puede deshabilitar al usuario porque es el único "
                        + "administrador habilitado del congreso '"
                        + participation.getCongress().getName() + "'."
                );
            }
        }
    }
    
    @Override
    @Transactional
    public MessageResponseDTO reactivateParticipantByAdmin(
            String congressCode,
            String userCode,
            CongressParticipantInstitutionRequestDTO requestDTO) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );

        // 4. Validar que el administrador tenga una institución definida
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Validar la institución proporcionada
        if (requestDTO == null
                || requestDTO.getInstitution() == null
                || requestDTO.getInstitution().isBlank()) {
            throw new ArgumentNotValidException(
                    "La institución es obligatoria para reactivar la participación."
            );
        }

        // 6. Buscar la participación, independientemente de su estado
        CongressParticipant participant =
                congressParticipantRepository.findByUserCodeAndCongressCode(
                        userCode,
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una participación previa para el usuario "
                        + userCode + " en el congreso: " + congressCode
                ));

        // 7. Verificar que la participación esté inactiva
        if (participant.isActive()) {
            throw new ArgumentNotValidException(
                    "El usuario ya tiene una participación activa en este congreso."
            );
        }

        // 8. Reactivar la participación y actualizar la institución
        participant.setActive(true);
        participant.setInstitution(requestDTO.getInstitution().strip());

        // 9. Guardar los cambios
        congressParticipantRepository.save(participant);

        // 10. Retornar confirmación
        return MessageResponseDTO.builder()
                .message(
                        "La participación de "
                        + participant.getUser().getFirstName() + " "
                        + participant.getUser().getLastName()
                        + " en el congreso '" + congress.getName()
                        + "' ha sido reactivada exitosamente."
                )
                .build();
    }
    
    @Override
    @Transactional
    public void updateParticipantRole(String congressCode, String userCode, RoleName newRole) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // 3. Validar permisos de administración
        authorizationService.validateCongressAdminPermission(
                authenticatedUser,
                congressCode
        );
        
        // 4. Validar que el administrador tenga una institución definida en este congreso
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        // 5. Validar que el nuevo rol pueda asignarse
        roleService.validateAssignableRole(newRole);

        // 6. Buscar la participación activa del usuario
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        userCode,
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa para el usuario "
                        + userCode + " en el congreso: " + congressCode
                ));

        // 7. Obtener el rol solicitado
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El rol solicitado no se encuentra configurado en el sistema."
                ));

        // 8. Verificar que el congreso no se quede sin administradores
        boolean isCurrentlyAdmin =
                participant.getRole().getName() == RoleName.ADMINISTRATOR;

        boolean isRemovingAdminRole =
                isCurrentlyAdmin && newRole != RoleName.ADMINISTRATOR;

        if (isRemovingAdminRole) {

            long activeAdmins =
                    congressParticipantRepository.countActiveParticipantsByCongressCodeAndRole(
                            congressCode,
                            RoleName.ADMINISTRATOR
                    );

            if (activeAdmins <= 1) {
                throw new ArgumentNotValidException(
                        "No se puede quitar el rol de administrador al último "
                        + "administrador activo del congreso."
                );
            }
        }

        // 9. Actualizar el rol
        participant.setRole(role);

        // 10. Guardar los cambios
        congressParticipantRepository.save(participant);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CongressParticipantResponseDTO getAuthenticatedParticipantInCongress(String congressCode) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        congressService.getCongressEntityByCode(congressCode);

        // 3. Buscar la participación activa del usuario autenticado
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        authenticatedUser.getCode(),
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa del usuario autenticado "
                        + "en el congreso: " + congressCode
                ));

        // 4. Convertir la entidad a DTO y retornar
        return congressParticipantMapper.toCongressParticipantResponseDTO(participant);
    }
    
    @Override
    @Transactional
    public MessageResponseDTO updateAuthenticatedParticipantInstitution(String congressCode, CongressParticipantInstitutionRequestDTO requestDTO) {

        // 1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        // 2. Verificar que el congreso exista
        Congress congress = congressService.getCongressEntityByCode(congressCode);

        // 3. Buscar la participación activa del usuario autenticado
        CongressParticipant participant =
                congressParticipantRepository.findActiveParticipantByUserCodeAndCongressCode(
                        authenticatedUser.getCode(),
                        congressCode
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una inscripción activa del usuario autenticado "
                        + "en el congreso: " + congressCode
                ));

        // 4. Actualizar la institución
        participant.setInstitution(requestDTO.getInstitution().strip());

        // 5. Guardar los cambios
        congressParticipantRepository.save(participant);

        // 6. Retornar confirmación
        return MessageResponseDTO.builder()
                .message(
                        "Tu institución ha sido actualizada exitosamente en el congreso '"
                        + congress.getName() + "'."
                )
                .build();
    }
}
